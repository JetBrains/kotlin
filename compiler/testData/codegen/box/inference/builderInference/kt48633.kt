// DONT_TARGET_EXACT_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
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