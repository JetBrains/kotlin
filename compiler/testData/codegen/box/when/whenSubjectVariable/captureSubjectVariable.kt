// !LANGUAGE: +VariableDeclarationInWhenSubject

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