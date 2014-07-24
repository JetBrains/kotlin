import  test.*

val s = doWork({11})

fun box(): String {
    if (s != 11) return "test1: ${s}"

    return "OK"
}