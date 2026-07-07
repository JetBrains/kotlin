// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// RENDER_DIAGNOSTIC_ARGUMENTS
sealed interface PointerType {
    data object Mouse : PointerType
    data object Touch : PointerType
    data object Stylus : PointerType
    data object Eraser : PointerType
}

data object PointerEvent

fun interface PointerMatcher {

    fun matches(event: PointerEvent): Boolean

    operator fun plus(pointerMatcher: PointerMatcher): PointerMatcher {
        val sources = buildList {
            for (matcher in listOf(this@PointerMatcher, pointerMatcher)) {
                if (matcher is CombinedPointerMatcher)
                    addAll(matcher.sources)
                else
                    add(matcher)
            }
        }
        return CombinedPointerMatcher(sources)
    }

    companion object {
        fun pointer(
            pointerType: PointerType,
        ): PointerMatcher = PointerTypeAndButtonMatcher(pointerType)

        fun mouse(): PointerMatcher = pointer(PointerType.Mouse)

        fun stylus(): PointerMatcher =
            pointer(PointerType.Stylus)

        val stylus: PointerMatcher = stylus()

        val touch: PointerMatcher = PointerTypeAndButtonMatcher(PointerType.Touch)

        val eraser: PointerMatcher =
            PointerTypeAndButtonMatcher(PointerType.Eraser, matchAllButtons = true)

        private class PointerTypeAndButtonMatcher(
            val pointerType: PointerType,
            val matchAllButtons: Boolean = false,
        ) : PointerMatcher {
            override fun matches(event: PointerEvent): Boolean {
                return matchAllButtons
            }
        }

        private class CombinedPointerMatcher(val sources: List<PointerMatcher>) : PointerMatcher {
            override fun matches(event: PointerEvent): Boolean {
                return sources.any { it.matches(event) }
            }
        }

        /**
         * The Primary [PointerMatcher] covers the most common cases of pointer inputs.
         * [Primary] will match [PointerEvent]s, which match at least one of the following conditions:
         * - [PointerType] is [PointerType.Mouse] and [PointerEvent.button] is [PointerButton.Primary]
         * - [PointerType] is [PointerType.Touch]
         * - [PointerType] is [PointerType.Stylus], no buttons pressed
         * - [PointerType] is [PointerType.Eraser]
         */
        val Primary: PointerMatcher = CombinedPointerMatcher(
            listOf(
                mouse(),
                touch,
                stylus,
                eraser
            )
        )
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, forLoop, funInterface, functionDeclaration, ifExpression,
interfaceDeclaration, isExpression, lambdaLiteral, localProperty, nestedClass, objectDeclaration, operator, override,
primaryConstructor, propertyDeclaration, sealed, smartcast, thisExpression */
