import java.io.*

fun box(): String {
    val ACCEPT_NAME = "test"
    val WRONG_NAME = "wrong"

    val result = JavaClass.invokeFilter({ file -> ACCEPT_NAME == file?.getName() }, File(ACCEPT_NAME), File(WRONG_NAME))

    if (result != "true false") return "Wrong result: $result"
    return "OK"
}
