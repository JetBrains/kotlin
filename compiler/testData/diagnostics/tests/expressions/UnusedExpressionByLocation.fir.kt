// RUN_PIPELINE_TILL: BACKEND
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

    when {
        else -> Unit
    }

    when (a) {
        "" -> Unit
    }

    when {
        else -> {
            <!UNUSED_EXPRESSION!>Unit<!>
        }
    }

    when {
        else -> when {
            else -> {
                <!UNUSED_EXPRESSION!>Unit<!>
            }
        }
    }
}

fun testIfElse(
    x: String?,
    transformer: (String) -> Int,
) {
    val a =
        if (x == null) transformer
        else { str -> str.length }
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
        Unit
    }

    consume {
        <!UNUSED_EXPRESSION!>""<!>
        ""
    }

    consume {
        <!UNUSED_EXPRESSION!>""<!>
        Unit
    }

    consume<Unit> {
        <!UNUSED_EXPRESSION!>""<!>
    }

    consume<Unit> {
        <!UNUSED_EXPRESSION!>""<!>
        Unit
    }
}
