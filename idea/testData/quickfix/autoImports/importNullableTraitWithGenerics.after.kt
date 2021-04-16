import importTrait.data.TestTrait

// "Import" "true"
// FIR_IGNORE
// ERROR: Unresolved reference: TestTrait

fun test() {
    val a: TestTrait<String, Int>? = null
}
