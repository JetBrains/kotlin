// WITH_STDLIB

val String.myLength get() = this.length

fun box(): String {
        if (listOf("abc", "de", "f").map(String::length) != listOf(3, 2, 1)) return "Fail 1"
        if (listOf("abc", "de", "f").map(String::myLength) != listOf(3, 2, 1)) return "Fail 2"
        return "OK"
}

