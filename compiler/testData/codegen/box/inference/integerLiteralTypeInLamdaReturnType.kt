// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// !LANGUAGE: +NewInference
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// SKIP_DCE_DRIVEN

class Foo<C : Any> {
    fun test(candidates: Collection<C>): List<C> {
        return candidates.sortedBy { 1 }
    }
}

fun box(): String {
    val foo = Foo<String>()

    val list = listOf("OK")
    val sorted = foo.test(list)

    return list.first()
}