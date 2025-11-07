// RUN_PIPELINE_TILL: BACKEND
// ISSUES: KT-75338

fun use(a: Any?) {}
fun invokeLater(block: () -> Unit) {}
inline fun invokeInline(block: () -> Unit) { block() }

fun test() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
    foo = 3
    fun usage() {
        use(foo)
    }

    foo = 5
    usage()
}

fun test2() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
    foo = 3
    invokeLater {
        use(foo)
    }

    foo = 5
}

fun test3() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
    foo = 3
    invokeLater {
        invokeLater {
            foo = 4
        }
        foo = 5
    }

    use(foo)

    invokeLater {
        foo = 6
        foo = 7
        use(foo)
        invokeLater {
            use(foo)
            invokeLater {
                foo = 8
            }
            foo = 9
        }
    }
}

fun test4() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
    foo = 3
    invokeLater {
        use(foo)
    }

    foo = 5
    foo = 6
    use(foo)
}

fun test5() {
    var foo = 1
    use(foo)
    invokeLater {
        foo = 2
        foo = 3
        use(foo)
        invokeLater {
            use(foo)
            invokeLater {
                foo = 4
            }
            foo = 5
        }
        foo = 6
        foo = 7
    }
    foo = 8
    foo = 9
}

fun test6() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    try {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
        invokeLater {
            foo = 4
        }
        <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 3
    } catch (_: Throwable) {
        foo = 4
        use(foo)
    } finally {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 5
        <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 6
    }
}

fun test7() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    foo = 2
    try {
        foo = 3
    } catch (_: Throwable) {
        invokeLater {
            use(foo)
            foo = 4
        }
        foo = 5
    } finally {
        foo = 6
    }
}

fun test8() {
    var foo = 1
    invokeLater { use(foo) }
    foo = 2
    try {
        foo = 3
    } catch (_: Throwable) {
        foo = 5
    } finally {
        foo = 6
    }
}

fun test9() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
    try {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 3
    } catch (_: Throwable) {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 5
    } finally {
        foo = 6
    }
    use(foo)
}

fun test10() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
    try {
        foo = 3
    } catch (_: Throwable) {
        foo = 5
    }
    use(foo)
}

fun test11() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
    try {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 3
        foo = 4
    } catch (t: Throwable) {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 5
        throw t
    }
    use(foo)
}

fun test12() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
    foo = 3
    try {
        foo = 4
        foo = 5
    } catch (t: Throwable) {
        foo = 6
        foo = 7
        throw t
    } finally {
        use(foo)
    }
}

fun test13() {
    var foo = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>1<!>
    <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
    invokeInline {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 3
    }
    foo = 4
    use(foo)
}

fun test14() {
    var foo = 1

    fun usage() {
        use(foo)
        try {
            <!ASSIGNED_VALUE_IS_NEVER_READ!>foo<!> = 2
        } finally {
            foo = 3
        }
    }
}

fun test15() {
    invokeLater {
        var foo = 1
        use(foo)
        invokeLater {
            foo = 2
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, inline, integerLiteral, lambdaLiteral,
localFunction, localProperty, nullableType, propertyDeclaration, tryExpression, unnamedLocalVariable */
