/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.expressions.expressionType
import org.jetbrains.kotlin.analysis.api.rendering.KaPiece
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRendererBuilder
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaTextAttribute
import org.jetbrains.kotlin.analysis.api.rendering.KaTypeApproximation
import org.jetbrains.kotlin.analysis.api.rendering.append
import org.jetbrains.kotlin.analysis.api.rendering.output
import org.jetbrains.kotlin.analysis.api.rendering.push
import org.jetbrains.kotlin.analysis.api.rendering.renderToString
import org.jetbrains.kotlin.analysis.api.scopes.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.scopes.staticDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.findClass
import org.jetbrains.kotlin.analysis.api.symbols.findPackage
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.withNullability
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests the behavior of [KaRenderingOption]s which cannot be covered by the generated renderer tests, along with other renderer API
 * surface, such as direct [KaType][org.jetbrains.kotlin.analysis.api.types.KaType] and
 * [KaConstantValue][org.jetbrains.kotlin.analysis.api.base.KaConstantValue] rendering, and rendering of symbols which cannot appear as
 * top-level declarations of a file.
 *
 * Simple value options are covered by test data directives instead (see
 * [RendererDirectives][org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer.RendererDirectives]).
 */
class RendererExecutionTest : AbstractAnalysisApiExecutionTest("testData/renderer") {
    override val configurator = LLSourceLikeTestConfigurator()

    @Test
    fun allowedKeywords(mainFile: KtFile) {
        analyze(mainFile) {
            val renderer = KaRenderer.default.copy {
                set(KaRenderingOption.AllowedKeywords) { keyword ->
                    keyword != KtTokens.SUSPEND_KEYWORD && keyword != KtTokens.PROTECTED_KEYWORD
                }
            }

            assertEquals(
                """
                abstract class Base {
                    abstract fun compute(): Int
                }
                """.trimIndent(),
                renderer.renderToString(mainFile.declaration("Base").symbol),
            )
        }
    }

    @Test
    fun modifiers(mainFile: KtFile) {
        analyze(mainFile) {
            val renderer = KaRenderer.default.copy {
                set(KaRenderingOption.Modifiers) { _, modifiers ->
                    modifiers.filterNot { it == KtTokens.OPEN_KEYWORD } + KtTokens.FINAL_KEYWORD
                }
            }

            assertEquals(
                """
                final class Base {
                    final fun action()
                }
                """.trimIndent(),
                renderer.renderToString(mainFile.declaration("Base").symbol),
            )
        }
    }

    @Test
    fun typeTransformation(mainFile: KtFile) {
        analyze(mainFile) {
            val renderer = KaRenderer.default.copy {
                set(KaRenderingOption.TypeTransformation) { type -> type.withNullability(false) }
            }

            assertEquals(
                "fun describe(value: String, numbers: List<Int>): Int",
                renderer.renderToString(mainFile.declaration("describe").symbol),
            )
        }
    }

    @Test
    fun annotationFilter(mainFile: KtFile) {
        analyze(mainFile) {
            val renderer = KaRenderer.default.copy {
                set(KaRenderingOption.Annotations) { annotated ->
                    annotated.annotations.filter { it.classId?.shortClassName?.asString() != "Dropped" }
                }
            }

            assertEquals(
                """
                @Kept
                fun annotated()
                """.trimIndent(),
                renderer.renderToString(mainFile.declaration("annotated").symbol),
            )
        }
    }

    @Test
    fun annotationsInline(mainFile: KtFile) {
        analyze(mainFile) {
            val renderer = KaRenderer.default.copy {
                set(KaRenderingOption.AnnotationsOnNewLine) { _ -> false }
            }

            assertEquals(
                """
                @Anno class Marked {
                    @Anno fun member()
                }
                """.trimIndent(),
                renderer.renderToString(mainFile.declaration("Marked").symbol),
            )
        }
    }

    @Test
    fun contextParametersInline(mainFile: KtFile) {
        analyze(mainFile) {
            val renderer = KaRenderer.default.copy {
                set(KaRenderingOption.ContextReceiversOnNewLine) { _ -> false }
            }

            assertEquals(
                "context(scope: Scope) fun action()",
                renderer.renderToString(mainFile.declaration("action").symbol),
            )
        }
    }

    @Test
    fun classMemberOrigins(mainFile: KtFile) {
        analyze(mainFile) {
            val renderer = alphabeticallyOrderingRenderer {
                set(KaRenderingOption.ClassMemberOrigins, emptySet())
            }

            // `equals`, `hashCode`, and `toString` are not part of the declared member scope, so only `copy` and the `componentN`
            // functions appear among the generated members.
            assertEquals(
                """
                data class Point(val x: Int, val y: Int) {
                    fun copy(x: Int = ..., y: Int = ...): Point

                    operator fun component1(): Int

                    operator fun component2(): Int
                }
                """.trimIndent(),
                renderer.renderToString(mainFile.declaration("Point").symbol),
            )
        }
    }

    @Test
    fun classMembers(mainFile: KtFile) {
        analyze(mainFile) {
            val renderer = alphabeticallyOrderingRenderer {
                set(KaRenderingOption.ClassMembers) { classSymbol ->
                    classSymbol.declaredMemberScope.callables.filterIsInstance<KaNamedFunctionSymbol>().toList()
                }
            }

            assertEquals(
                """
                class Mixed {
                    fun first()

                    fun second()
                }
                """.trimIndent(),
                renderer.renderToString(mainFile.declaration("Mixed").symbol),
            )
        }
    }

    @Test
    fun classMemberOrdering(mainFile: KtFile) {
        analyze(mainFile) {
            val renderer = KaRenderer.default.copy {
                set(KaRenderingOption.ClassMemberOrdering) { first, second ->
                    -KaRenderer.default.renderToString(first).compareTo(KaRenderer.default.renderToString(second))
                }
            }

            assertEquals(
                """
                class Ordered {
                    val gamma: Int

                    fun beta()

                    fun alpha()
                }
                """.trimIndent(),
                renderer.renderToString(mainFile.declaration("Ordered").symbol),
            )
        }
    }

    @Test
    fun standaloneSymbols(mainFile: KtFile) {
        analyze(mainFile) {
            val container = mainFile.declaration("container").symbol as KaNamedFunctionSymbol
            assertEquals("T : CharSequence", KaRenderer.default.renderToString(container.typeParameters.single()))
            assertEquals("parameter: String", KaRenderer.default.renderToString(container.valueParameters[0]))
            assertEquals("vararg rest: Int", KaRenderer.default.renderToString(container.valueParameters[1]))

            val extension = mainFile.declaration("extension").symbol as KaNamedFunctionSymbol
            assertEquals("String.", KaRenderer.default.renderToString(extension.receiverParameter!!))

            val locals = PsiTreeUtil.findChildrenOfType(mainFile, KtProperty::class.java).filter { it.isLocal }
            assertEquals("lateinit var mutable: String", KaRenderer.default.renderToString(locals.single { it.name == "mutable" }.symbol))
            assertEquals("val immutable: Int", KaRenderer.default.renderToString(locals.single { it.name == "immutable" }.symbol))

            val lambda = PsiTreeUtil.findChildOfType(mainFile, KtLambdaExpression::class.java)!!
            assertEquals("fun (value: Int): Int", KaRenderer.default.renderToString(lambda.functionLiteral.symbol))

            val property = (mainFile.declaration("WithField").symbol as KaNamedClassSymbol)
                .declaredMemberScope.callables.filterIsInstance<KaKotlinPropertySymbol>().single()
            assertEquals("field: Int", KaRenderer.default.renderToString(property.backingFieldSymbol!!))

            assertEquals("package kotlin", KaRenderer.default.renderToString(findPackage(FqName("kotlin"))!!))

            val javaHolder = findClass(ClassId.topLevel(FqName("JavaHolder")))!!
            val javaFields = (javaHolder.declaredMemberScope.callables + javaHolder.staticDeclaredMemberScope.callables)
                .filterIsInstance<KaJavaFieldSymbol>()
                .associateBy { it.name.asString() }
            // The type of `CONSTANT` is non-flexible, as a `static final` field with a constant initializer is enhanced to non-null.
            assertEquals("static val CONSTANT: String", KaRenderer.default.renderToString(javaFields.getValue("CONSTANT")))
            assertEquals("static var mutableText: String!", KaRenderer.default.renderToString(javaFields.getValue("mutableText")))
            // The Java `protected` maps to the protected-and-package visibility, which has no Kotlin modifier, so it is not rendered.
            assertEquals("var counter: Int", KaRenderer.default.renderToString(javaFields.getValue("counter")))
        }
    }

    @Test
    fun directRendering(mainFile: KtFile) {
        analyze(mainFile) {
            val provide = mainFile.declaration("provide").symbol as KaNamedFunctionSymbol
            assertEquals("List<String>?", KaRenderer.default.renderToString(provide.returnType))

            val answer = mainFile.declaration("ANSWER").symbol as KaKotlinPropertySymbol
            val initializer = answer.initializer as KaConstantInitializerValue

            val output = KaRenderingOutput.plainString()
            KaRenderer.default.render(initializer.constant, KaPiece.ConstantValue, output)

            assertEquals("42", output.toString())
        }
    }

    @Test
    fun nonDenotableTypes(mainFile: KtFile) {
        analyze(mainFile) {
            val locals = PsiTreeUtil.findChildrenOfType(mainFile, KtProperty::class.java).filter { it.isLocal }
            val capturedType = locals.single { it.name == "capturedResult" }.initializer!!.expressionType!!
            val intersectionType = locals.single { it.name == "intersectionResult" }.initializer!!.expressionType!!

            assertEquals("Captured(out Number)", KaRenderer.default.renderToString(capturedType))
            assertEquals("Comparable<*> & Number", KaRenderer.default.renderToString(intersectionType))

            val approximatingRenderer = KaRenderer.default.copy {
                set(KaRenderingOption.TypeApproximation, KaTypeApproximation.TO_DENOTABLE_SUPERTYPE)
            }

            assertEquals("Number", approximatingRenderer.renderToString(capturedType))
            assertEquals("Any", approximatingRenderer.renderToString(intersectionType))
        }
    }

    @Test
    fun customPieceRenderer(mainFile: KtFile) {
        analyze(mainFile) {
            val renderer = KaRenderer.default.copy {
                push(KaPiece.FunctionBody) { output.append(" { compiled code }", KaTextAttribute.Comment) }
            }

            assertEquals(
                "fun withBody(): Int { compiled code }",
                renderer.renderToString(mainFile.declaration("withBody").symbol),
            )

            val classNameRenderer = KaRenderer.default.copy {
                push(KaPiece.ClassName) { classId -> output.append(classId.asFqNameString(), KaTextAttribute.Identifier) }
            }

            assertEquals(
                """
                @kotlin.Deprecated(message = "out")
                fun annotated()
                """.trimIndent(),
                classNameRenderer.renderToString(mainFile.declaration("annotated").symbol),
            )
        }
    }

    /** A renderer which orders class members alphabetically by their rendered text, for deterministic member-list assertions. */
    private fun alphabeticallyOrderingRenderer(customizations: KaRendererBuilder.() -> Unit): KaRenderer =
        KaRenderer.default.copy {
            set(KaRenderingOption.ClassMemberOrdering) { first, second ->
                KaRenderer.default.renderToString(first).compareTo(KaRenderer.default.renderToString(second))
            }
            customizations()
        }

    private fun KtFile.declaration(name: String): KtDeclaration =
        declarations.single { it.name == name }
}
