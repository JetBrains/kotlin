// FIR_IDENTICAL
// LANGUAGE: -ProperForInArrayLoopRangeVariableAssignmentSemantic
// DIAGNOSTICS: -UNUSED_VALUE
// SKIP_TXT

class Delegate<T>(var v: T) {
    operator fun getValue(thisRef: Any?, kProp: Any?) = v
    operator fun setValue(thisRef: Any?, kProp: Any?, value: T) { v = value }
}

fun testLocalDelegatedProperty() {
    var xs by Delegate(arrayOf("a", "b", "c"))
    for (x in xs) {
        println(x)
        xs = arrayOf("d", "e", "f")
    }
}