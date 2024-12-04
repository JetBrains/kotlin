// WITH_STDLIB

fun box(): String {
    1L.mod("123a".indexOfAny("a".toCharArray()))
    return "OK"
}
