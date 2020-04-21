fun nestedNothing(s: String) =
    "" + if (s == "OK") s else barf()

fun barf(): Nothing = throw NullPointerException()

fun box() = nestedNothing("OK")