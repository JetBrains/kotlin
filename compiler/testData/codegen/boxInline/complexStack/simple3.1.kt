import test.*

fun box(): String {
    val result = processRecords { "B" + it }

    return if (result == "BOK1") "OK" else "fail: $result"
}