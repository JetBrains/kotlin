// ISSUE: KT-68975
// See same test for diagnostics: compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/inlinedReturnBreakContinue/lambdaPassedToInlineFunction.kt
// LANGUAGE: +BreakContinueInInlineLambdas
// WITH_STDLIB
// TARGET_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// REASON: SyntaxError: Undefined label 'outer'

import kotlin.test.assertEquals

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
public annotation class SomeAnnotation

inline fun foo(<!UNUSED_PARAMETER!>block<!>: () -> Unit) = js("block()")

fun box(): String {
    val visited = mutableListOf<Pair<Int, Int>>()

    var i = 0
    outer@ while (true) {
        i += 1
        inner@ for (j in 1..10) {
            foo(
                (@SomeAnnotation
                fun() {
                    foo(fun() {
                        foo(@SomeAnnotation {
                            foo {
                                if (i == 2) {
                                    continue@outer
                                }

                                if (i == 4) {
                                    break@outer
                                }

                                if (j == 2) {
                                    continue
                                }

                                if (j == 4) {
                                    continue@inner
                                }

                                if (j == 6 && i == 1) {
                                    break@inner
                                }

                                if (j == 6 && i == 3) {
                                    break
                                }
                            }
                        })
                    })
                })
            )
            visited += i to j
        }
    }

    <!UNREACHABLE_CODE!>assertEquals(listOf(1 to 1, 1 to 3, 1 to 5, 3 to 1, 3 to 3, 3 to 5), visited)<!>

    <!UNREACHABLE_CODE!>return "OK"<!>
}
