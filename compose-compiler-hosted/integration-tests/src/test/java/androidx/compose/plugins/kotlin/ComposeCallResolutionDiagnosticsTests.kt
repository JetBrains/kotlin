/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

class ComposeCallResolutionDiagnosticsTests : AbstractComposeDiagnosticsTest() {

    private var isSetup = false
    override fun setUp() {
        isSetup = true
        super.setUp()
    }

    private fun <T> ensureSetup(block: () -> T): T {
        if (!isSetup) setUp()
        return block()
    }

    private fun setupAndDoTest(text: String) = ensureSetup { doTest(text) }

    fun testImplicitlyPassedReceiverScope1() = setupAndDoTest(
        """
            import androidx.compose.*
            import android.widget.*
            import android.os.Bundle
            import android.app.Activity
            import android.widget.FrameLayout

            val x: Any? = null

            fun Activity.setViewContent(composable: @Composable() () -> Unit): CompositionContext? {
                assert(composable != x)
                return null
            }

            open class WebComponentActivity : Activity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)

                    setViewContent {
                        FrameLayout {
                        }
                    }
                }
            }
        """
    )

    fun testSimpleReceiverScope() = setupAndDoTest(
        """
            import android.widget.FrameLayout
            import androidx.compose.Composable

            class SomeScope {
             val composer get() = androidx.compose.composer
            }

            @Composable fun SomeScope.foo() {
                FrameLayout { }
            }

        """
    )

    fun testReceiverScopeComposer() = setupAndDoTest(
        """
            import androidx.compose.Applier
            import androidx.compose.ApplyAdapter
            import androidx.compose.Component
            import androidx.compose.Composable
            import androidx.compose.Composer
            import androidx.compose.ComposerUpdater
            import androidx.compose.CompositionContext
            import androidx.compose.CompositionReference
            import androidx.compose.Effect
            import androidx.compose.Recomposer
            import androidx.compose.SlotTable
            import androidx.compose.ViewValidator
            import androidx.compose.cache

            class TextSpan(
                val style: String? = null,
                val text: String? = null,
                val children: MutableList<TextSpan> = mutableListOf()
            )

            /**
             * This adapter is used by [TextSpanComposer] to build the [TextSpan] tree.
             * @see ApplyAdapter
             */
            internal class TextSpanApplyAdapter : ApplyAdapter<TextSpan> {
                override fun TextSpan.start(instance: TextSpan) {}

                override fun TextSpan.end(instance: TextSpan, parent: TextSpan) {}

                override fun TextSpan.insertAt(index: Int, instance: TextSpan) {
                    children.add(index, instance)
                }

                override fun TextSpan.removeAt(index: Int, count: Int) {
                    repeat(count) {
                        children.removeAt(index)
                    }
                }

                override fun TextSpan.move(from: Int, to: Int, count: Int) {
                    if (from == to) return

                    if (from > to) {
                        val moved = mutableListOf<TextSpan>()
                        repeat(count) {
                            moved.add(children.removeAt(from))
                        }
                        children.addAll(to, moved)
                    } else {
                        // Number of elements between to and from is smaller than count, can't move.
                        if (count > to - from) return
                        repeat(count) {
                            val node = children.removeAt(from)
                            children.add(to - 1, node)
                        }
                    }
                }
            }

            typealias TextSpanUpdater<T> = ComposerUpdater<TextSpan, T>

            /**
             * The composer of [TextSpan].
             */
            class TextSpanComposer internal constructor(
                root: TextSpan,
                recomposer: Recomposer
            ) : Composer<TextSpan>(SlotTable(), Applier(root, TextSpanApplyAdapter()), recomposer)

            @PublishedApi
            internal val invocation = Object()

            class TextSpanComposition(val composer: TextSpanComposer) {
                @Suppress("NOTHING_TO_INLINE")
                inline operator fun <V> Effect<V>.unaryPlus(): V = resolve(this@TextSpanComposition.composer)

                inline fun emit(
                    key: Any,
                    /*crossinline*/
                    ctor: () -> TextSpan,
                    update: TextSpanUpdater<TextSpan>.() -> Unit
                ) = with(composer) {
                    startNode(key)
                    @Suppress("UNCHECKED_CAST") val node = if (inserting) ctor().also { emitNode(it) }
                    else useNode()
                    TextSpanUpdater(this, node).update()
                    endNode()
                }

                inline fun emit(
                    key: Any,
                    /*crossinline*/
                    ctor: () -> TextSpan,
                    update: TextSpanUpdater<TextSpan>.() -> Unit,
                    children: () -> Unit
                ) = with(composer) {
                    startNode(key)
                    @Suppress("UNCHECKED_CAST")val node = if (inserting) ctor().also { emitNode(it) }
                    else useNode()
                    TextSpanUpdater(this, node).update()
                    children()
                    endNode()
                }

                @Suppress("NOTHING_TO_INLINE")
                inline fun joinKey(left: Any, right: Any?): Any = composer.joinKey(left, right)

                inline fun call(
                    key: Any,
                    /*crossinline*/
                    invalid: ViewValidator.() -> Boolean,
                    block: () -> Unit
                ) = with(composer) {
                    startGroup(key)
                    if (ViewValidator(composer).invalid() || inserting) {
                        startGroup(invocation)
                        block()
                        endGroup()
                    } else {
                        skipCurrentGroup()
                    }
                    endGroup()
                }

                inline fun <T> call(
                    key: Any,
                    /*crossinline*/
                    ctor: () -> T,
                    /*crossinline*/
                    invalid: ViewValidator.(f: T) -> Boolean,
                    block: (f: T) -> Unit
                ) = with(composer) {
                    startGroup(key)
                    val f = cache(true, ctor)
                    if (ViewValidator(this).invalid(f) || inserting) {
                        startGroup(invocation)
                        block(f)
                        endGroup()
                    } else {
                        skipCurrentGroup()
                    }
                    endGroup()
                }
            }

            /**
             * As the name indicates, [Root] object is associated with a [TextSpan] tree root. It contains
             * necessary information used to compose and recompose [TextSpan] tree. It's created and stored
             * when the [TextSpan] container is composed for the first time.
             */
            private class Root : Component() {
                fun update() = composer.compose()
                lateinit var scope: TextSpanScope
                lateinit var composer: CompositionContext
                lateinit var composable: @Composable() TextSpanScope.() -> Unit
                @Suppress("PLUGIN_ERROR")
                override fun compose() {
                    with(scope) {
                        composable()
                    }
                }
            }

            /**
             *  The map used store the [Root] object for [TextSpan] trees.
             */
            private val TEXTSPAN_ROOT_COMPONENTS = HashMap<TextSpan, Root>()

            /**
             * Get the [Root] object of the given [TextSpan] root node.
             */
            private fun getRootComponent(node: TextSpan): Root? {
                return TEXTSPAN_ROOT_COMPONENTS[node]
            }

            /**
             * Store the [Root] object of [node].
             */
            private fun setRoot(node: TextSpan, component: Root) {
                TEXTSPAN_ROOT_COMPONENTS[node] = component
            }

            /**
             * Compose a [TextSpan] tree.
             * @param container The root of [TextSpan] tree where the children TextSpans will be attached to.
             * @param parent The parent composition reference, if applicable. Default is null.
             * @param composable The composable function to compose the children of [container].
             * @see CompositionReference
             */
            @Suppress("PLUGIN_ERROR")
            fun compose(
                container: TextSpan,
                parent: CompositionReference? = null,
                composable: @Composable() TextSpanScope.() -> Unit
            ) {
                var root = getRootComponent(container)
                if (root == null) {
                    lateinit var composer: TextSpanComposer
                    root = Root()
                    setRoot(container, root)
                    root.composer = CompositionContext.prepare(root, parent) {
                        TextSpanComposer(container, this).also { composer = it }
                    }
                    root.scope = TextSpanScope(TextSpanComposition(composer))
                    root.composable = composable

                    root.update()
                } else {
                    root.composable = composable

                    root.update()
                }
            }

            /**
             * Cleanup when the [TextSpan] is no longer used.
             *
             * @param container The root of the [TextSpan] to be disposed.
             * @param parent The [CompositionReference] used together with [container] when [composer] is
             * called.
             */
            fun disposeComposition(
                container: TextSpan,
                parent: CompositionReference? = null
            ) {
                // temporary easy way to call correct lifecycles on everything
                compose(container, parent) {}
                TEXTSPAN_ROOT_COMPONENTS.remove(container)
            }

            /**
             * The receiver class of the children of Text and TextSpan. Such that [Span] can only be used
             * within [Text] and [TextSpan].
             */
            class TextSpanScope internal constructor(val composer: TextSpanComposition)

            @Composable
            fun TextSpanScope.Span(
                text: String? = null,
                style: String? = null,
                child: @Composable TextSpanScope.() -> Unit
            ) {
                TextSpan(text = text, style = style) {
                    child()
                }
            }

            @Composable
            fun TextSpanScope.Span(
                text: String? = null,
                style: String? = null
            ) {
                TextSpan(text = text, style = style)
            }
        """
    )
}
