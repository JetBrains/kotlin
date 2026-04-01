// FILE: main.kt
fun box() =
        if (getAndCheck({ 42 }, { 42 })) "OK" else "fail"

// FILE: lib.kt
inline fun <T> getAndCheck(getFirst: () -> T, getSecond: () -> T) =
        getFirst() == getSecond()