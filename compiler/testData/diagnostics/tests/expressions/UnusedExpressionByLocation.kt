// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_EXTRA_CHECKERS
// DIAGNOSTICS: +UNUSED_EXPRESSION, +UNUSED_LAMBDA_EXPRESSION, -UNUSED_VARIABLE

fun run(block: () -> Unit) {}
fun <T> consume(block: () -> T) {}

class TestClass() {
    val testLambda1 = run {
        "" // actually unused
    }

    val testLambda2 = consume {
        <!UNUSED_EXPRESSION!>""<!>
        ""
    }

    val testWhen = when {
        else -> {
            <!UNUSED_EXPRESSION!>""<!>
            ""
        }
    }

    val testTry = try {
        <!UNUSED_EXPRESSION!>""<!>
        ""
    } catch (_: Exception) {
        <!UNUSED_EXPRESSION!>""<!>
        ""
    } finally {
        <!UNUSED_EXPRESSION!>""<!>
    }

    init {
        <!UNUSED_EXPRESSION!>""<!>
    }
}

fun testFun() {
    <!UNUSED_EXPRESSION!>""<!>
}

fun testWhen() {
    when {
        else -> {
            <!UNUSED_EXPRESSION!>""<!>
        }
    }

    val a = when {
        else -> {
            <!UNUSED_EXPRESSION!>""<!>
            ""
        }
    }
}

fun testTry() {
    try {
        <!UNUSED_EXPRESSION!>""<!>
    } catch (_: Exception) {
        <!UNUSED_EXPRESSION!>""<!>
    } finally {
        <!UNUSED_EXPRESSION!>""<!>
    }

    val a = try {
        <!UNUSED_EXPRESSION!>""<!>
        ""
    } catch (_: Exception) {
        <!UNUSED_EXPRESSION!>""<!>
        ""
    } finally {
        <!UNUSED_EXPRESSION!>""<!>
    }
}

fun testLambda() {
    <!UNUSED_LAMBDA_EXPRESSION!>{
        <!UNUSED_EXPRESSION!>""<!>
        Unit
    }<!>

    run {
        <!UNUSED_EXPRESSION!>""<!>
    }

    run {
        <!UNUSED_EXPRESSION!>""<!>
        Unit // actually unused
    }

    consume {
        <!UNUSED_EXPRESSION!>""<!>
        ""
    }

    consume {
        <!UNUSED_EXPRESSION!>""<!>
        Unit // actually unused
    }

    consume<Unit> {
        <!UNUSED_EXPRESSION!>""<!>
    }

    consume<Unit> {
        <!UNUSED_EXPRESSION!>""<!>
        Unit // actually unused
    }
}
