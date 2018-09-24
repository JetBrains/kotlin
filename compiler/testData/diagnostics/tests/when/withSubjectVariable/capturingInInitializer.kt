// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun getBoolean() = true

fun testSafeCaptureVarInInitializer() {
    var x: Int? = 42
    x!!
    <!DEBUG_INFO_SMARTCAST!>x<!>.inc()

    val s = when (val y = run { x = 42; 32 }) {
        0 -> {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() // TODO fix smart casts for captured variables
            "0"
        }
        else -> "!= 0"
    }

    <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() // TODO fix smart casts for captured variables
}


fun testUnsafeCaptureVarInInitializer() {
    var x: Int? = 42
    x!!
    <!DEBUG_INFO_SMARTCAST!>x<!>.inc()

    val s = when (val y = run { x = null; 32 }) {
        0 -> {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() // NB smart cast should be impossible
            "0"
        }
        else -> "!= 0"
    }

    <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() // NB smart cast should be impossible
}