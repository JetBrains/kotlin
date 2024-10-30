// ISSUE: KT-72446
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// MODULE: lib
// FILE: lib.kt
inline fun test(
    block: () -> String = {
        var result = "O"
        val temp = { result += "K" }
        temp()
        result
    }
) = block()

// MODULE: main(lib)
// FILE: main.kt
fun box() = test()
