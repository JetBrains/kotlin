// RUN_PIPELINE_TILL: FRONTEND
// DUMP_CFG
interface A {
    fun foo()
}

interface B {
    fun bar()
}

fun test_1(x: Any?) {
    when {
        x is A -> x.foo()
        x is B -> x.bar()
    }

    when {
        x !is A -> {}
        x !is B -> x.foo()
        <!IMPOSSIBLE_IS_CHECK_ERROR!>x is Int<!> -> {
            x.foo()
            x.bar()
            x.inc()
        }
        else -> {
            x.foo()
            x.bar()
        }
    }
}

fun test_2(x: Any?) {
    when(x) {
        is A -> x.foo()
        is B -> x.bar()
    }

    when(x) {
        !is A -> {}
        !is B -> x.foo()
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is Int<!> -> {
            x.foo()
            x.bar()
            x.inc()
        }
        else -> {
            x.foo()
            x.bar()
        }
    }
}

fun test_3(x: Any?) {
    when(val y = x) {
        is A -> {
            x.foo()
            y.foo()
        }
        is B -> {
            x.bar()
            y.bar()
        }
    }

    when(val y = x) {
        !is A -> {}
        !is B -> {
            x.foo()
            y.foo()
        }
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is Int<!> -> {
            x.foo()
            x.bar()
            x.inc()
            y.foo()
            y.bar()
            y.inc()
        }
        else -> {
            x.foo()
            x.bar()
            y.foo()
            y.bar()
        }
    }
}

fun test_4(x: Any) {
    when (x as Int) {
        1 -> x.inc()
    }
    x.inc()
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, interfaceDeclaration, intersectionType,
isExpression, localProperty, nullableType, propertyDeclaration, smartcast, whenExpression, whenWithSubject */
