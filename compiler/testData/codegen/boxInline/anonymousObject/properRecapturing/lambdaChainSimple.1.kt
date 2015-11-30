import test.*

fun box(): String {
    val param = "start"
    var result = "fail"

    inlineFun("2") { a ->
        {
            result = param + a
        }()
    }


    return if (result == "start2") "OK" else "fail: $result"
}
