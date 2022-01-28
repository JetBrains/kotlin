// FIR_IDENTICAL
// ISSUE: KT-51009

fun test(b: Boolean, f: () -> String?): () -> String {
    val foo = try {
        f
    } catch (e: Exception) {
        return { "1" } // Infer return type of test () -> String instead of type of `f`
    }
    return { "2" }
}

fun test2(b: Boolean, f: () -> String?) = run { // implicit return type
    val foo = try {
        f
    } catch (e: Exception) {
        return { "1" }
    }
    { "2" }
}
