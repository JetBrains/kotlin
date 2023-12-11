// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun getBoolean() = true

fun testSafeCaptureVarInInitializer() {
    var x: Int? = 42
    x!!
    x.inc()

    val s = when (val y = run { x = 42; 32 }) {
        0 -> {
            x<!UNSAFE_CALL!>.<!>inc() // TODO fix smart casts for captured variables
            "0"
        }
        else -> "!= 0"
    }

    x<!UNSAFE_CALL!>.<!>inc() // TODO fix smart casts for captured variables
}


fun testUnsafeCaptureVarInInitializer() {
    var x: Int? = 42
    x!!
    x.inc()

    val s = when (val y = run { x = null; 32 }) {
        0 -> {
            x<!UNSAFE_CALL!>.<!>inc() // NB smart cast should be impossible
            "0"
        }
        else -> "!= 0"
    }

    x<!UNSAFE_CALL!>.<!>inc() // NB smart cast should be impossible
}