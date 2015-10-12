fun box(): String =
        if (listOf("abc", "de", "f").map(String::length.getter) == listOf(3, 2, 1)) "OK" else "Fail"
