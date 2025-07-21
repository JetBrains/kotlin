// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78942
// RENDER_DIAGNOSTICS_FULL_TEXT
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun <T> foo(t: () -> T): T = t()
fun <T> fooSame(t: (T) -> T): T = null!!
fun <A, B> fooDifferent(t: (A) -> B): B = null!!

fun testFoo() {
    val bar: String = <!TYPE_MISMATCH!>foo {
        <!TYPE_MISMATCH!>fun() = <!TYPE_MISMATCH!>""<!><!>
    }<!>

    foo<String> {
        <!TYPE_MISMATCH!>fun() = <!TYPE_MISMATCH!>""<!><!>
    }

    fooSame<String> {
        <!TYPE_MISMATCH, TYPE_MISMATCH!>fun() = ""<!>
    }

    val barSame: String = fooSame {
        <!TYPE_MISMATCH, TYPE_MISMATCH!>fun() = it<!>
    }

    fooSame { it: String ->
        <!TYPE_MISMATCH!>fun() = <!TYPE_MISMATCH!>it<!><!>
    }

    fooDifferent<Int, String> { it->
        <!TYPE_MISMATCH!>fun() = <!TYPE_MISMATCH!>it.toString()<!><!>
    }

    val barDifferent: String = <!TYPE_MISMATCH!>fooDifferent { it: Int ->
        <!TYPE_MISMATCH!>fun() = <!TYPE_MISMATCH!>it.toString()<!><!>
    }<!>
}

/* GENERATED_FIR_TAGS: anonymousFunction, checkNotNullCall, functionDeclaration, functionalType, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter */
