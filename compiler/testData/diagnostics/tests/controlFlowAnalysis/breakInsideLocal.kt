// LANGUAGE: +BreakContinueInInlineLambdas

inline fun <T> foo(block: () -> T): T  = block()


@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
public annotation class SomeAnnotation

fun test() {
    while (true) {
        fun local1(tag: Int) {
            when(tag) {
                0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }
                2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                3 -> foo(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                4 -> foo(@SomeAnnotation fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
            }
        }
    }
}

fun test2() {
    while (true) {
        <!UNUSED_LAMBDA_EXPRESSION!>{tag: Int ->
            when(tag) {
                0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
                1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> }
                2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> })
                3 -> foo(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> })
                4 -> foo(@SomeAnnotation fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> })
            }
        }<!>
    }
}

fun test3() {
    while (true) {
        class LocalClass(val tag: Int) {
            init {
                when(tag) {
                    0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
                    1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> }
                    2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> })
                    3 -> foo(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> })
                    4 -> foo(@SomeAnnotation fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> })
                }
            }

            fun foo() {
                when(tag) {
                    0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                    1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }
                    2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                    3 -> foo(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                    4 -> foo(@SomeAnnotation fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                }
            }
        }
    }
}

fun test4(tag: Int) {
    while (true) {
        object: Any() {
            init {
                when(tag) {
                    0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                    1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }
                    2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                    3 -> foo(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                    4 -> foo(@SomeAnnotation fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                }
            }
        }
    }
}

fun test5() {
    while (true) {
        class LocalClass(val s: String) {
            constructor(tag: Int) : this("") {
                when(tag) {
                    0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                    1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }
                    2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                    3 -> foo(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                    4 -> foo(@SomeAnnotation fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                }
            }
        }
    }
}

fun test6() {
    while (true) {
        class LocalClass(val tag: Int) {
            init {
                when(tag) {
                    0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                    1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }
                    2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                    3 -> foo(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                    4 -> foo(@SomeAnnotation fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                }
            }
            init {
                when(tag) {
                    0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
                    1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> }
                    2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> })
                    3 -> foo(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> })
                    4 -> foo(@SomeAnnotation fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> })
                }
            }
        }
    }
}

fun test7() {
    while (true) {
        class LocalClass(val tag: Int) {
            val x: Int = if (true) {
                when(tag) {
                    0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                    1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }
                    2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                    3 -> foo(fun (): Int { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>)
                    4 -> foo(@SomeAnnotation fun (): Int { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>)
                    else -> 1
                }
            }
            else {
                when(tag) {
                    0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
                    1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> }
                    2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> })
                    3 -> foo(fun (): Int { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>)
                    4 -> foo(@SomeAnnotation fun (): Int { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>)
                    else -> 2
                }
            }
        }
    }
}

fun test8() {
    while (true) {
        class LocalClass(val x: Int) {
            constructor(tag: Int, <!UNUSED_PARAMETER!>unused<!>: Boolean) : this(
                when(tag) {
                    0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                    1 -> foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }
                    2 -> foo(@SomeAnnotation { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
                    3 -> foo(fun (): Int { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>)
                    4 -> foo(@SomeAnnotation fun (): Int { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>)
                    else -> 1
                }
            )
        }
    }
}
