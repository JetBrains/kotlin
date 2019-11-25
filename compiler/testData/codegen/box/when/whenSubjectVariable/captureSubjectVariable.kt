// !LANGUAGE: +VariableDeclarationInWhenSubject
// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    var y: String = "OK"

    var materializer: (() -> String)? = null

    when (val x = y) {
        "OK" -> materializer = { x }
        else -> return "x is $x"
    }

    y = "Fail"

    return materializer!!.invoke()
}