// MODULE: lib
// FILE: lib.kt
@PublishedApi
internal fun published() = "OK"

// MODULE: main()(lib)()
// FILE: main.kt
inline fun callTest() = published()

fun box() = callTest()
