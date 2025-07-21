// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78942
// RENDER_DIAGNOSTICS_FULL_TEXT
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun <T> foo(t: () -> T): T = t()
fun <T> fooSame(t: (T) -> T): T = null!!
fun <A, B> fooDifferent(t: (A) -> B): B = null!!

fun testFoo() {
    val bar: String = foo {
        <!RETURN_TYPE_MISMATCH!>fun() = ""<!>
    }

    foo<String> {
        <!RETURN_TYPE_MISMATCH!>fun() = ""<!>
    }

    fooSame<String> {
        <!RETURN_TYPE_MISMATCH!>fun() = ""<!>
    }

    val barSame: String = fooSame {
        <!RETURN_TYPE_MISMATCH!>fun() = it<!>
    }

    fooSame { it: String ->
        <!RETURN_TYPE_MISMATCH!>fun() = it<!>
    }

    fooDifferent<Int, String> { it->
        <!RETURN_TYPE_MISMATCH!>fun() = it.toString()<!>
    }

    val barDifferent: String = fooDifferent { it: Int ->
        <!RETURN_TYPE_MISMATCH!>fun() = it.toString()<!>
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, checkNotNullCall, functionDeclaration, functionalType, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter */
