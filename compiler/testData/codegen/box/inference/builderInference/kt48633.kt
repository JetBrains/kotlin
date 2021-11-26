// DONT_TARGET_EXACT_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER on buildList call (E)
// WITH_STDLIB

class TowerDataElementsForName() {
    @OptIn(ExperimentalStdlibApi::class)
    val reversedFilteredLocalScopes = buildList {
        class Foo {
            val reversedFilteredLocalScopes = {
                add("OK")
            }
        }
        Foo().reversedFilteredLocalScopes()
    }
}

fun box(): String {
    return TowerDataElementsForName().reversedFilteredLocalScopes[0]
}