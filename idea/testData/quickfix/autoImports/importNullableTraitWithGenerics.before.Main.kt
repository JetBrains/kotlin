// "Import" "true"
// FIR_IGNORE
// ERROR: Unresolved reference: TestTrait

fun test() {
    val a: <caret>TestTrait<String, Int>? = null
}
