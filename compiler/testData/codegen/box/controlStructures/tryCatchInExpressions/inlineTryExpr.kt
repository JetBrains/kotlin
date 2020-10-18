// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
inline fun <T> tryOrElse(f1: () -> T, f2: () -> T): T =
        try { f1() } catch (e: Exception) { f2() }

fun testIt() =
        "abc" +
        tryOrElse({ try { "def" } catch(e: Exception) { "oops!" } }, { "hmmm..." }) +
        "ghi"

fun box(): String {
    val test = testIt()
    if (test != "abcdefghi") return "Failed, test==$test"

    return "OK"
}
