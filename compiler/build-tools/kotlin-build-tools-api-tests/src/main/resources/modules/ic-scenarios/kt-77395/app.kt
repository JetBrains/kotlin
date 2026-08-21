fun perform(): Result<Int, String> {
    return Result.Ok(value = 42)
}

fun stuff() {
    val staff = 111 // TODO: change me and run main() again
    val result = perform()
    if (result.isError()) {
        println(result.error)
        return
    }
    val value = result.value
    println(value)
}

fun main() {
    stuff()
}
