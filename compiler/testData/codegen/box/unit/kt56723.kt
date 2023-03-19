fun foo(f: () -> Unit, returnIfOk: String): String {
    val string = f().toString()

    return if (string == "kotlin.Unit") {
        returnIfOk
    } else {
        "FAIL: $string;"
    }
}

class Wrapper(var s: String)

fun box(): String {
    val w: Wrapper? = Wrapper("Test")

    val lambda = {
        w?.s = "X"
    }

    val w2: Wrapper? = null

    val lambda2 = {
        w2?.s = "X"
    }

    return foo(lambda, "O") + foo(lambda2, "K")
}
