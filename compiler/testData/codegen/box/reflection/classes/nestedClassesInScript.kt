// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR
// IGNORING_WASM_FOR_K2
// IGNORE_BACKEND: WASM
// FIR status: scripts aren't supported yet
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB
// WITH_REFLECT

// FILE: test.kt

fun box(): String {
    val kClass = Script::class
    val nestedClasses = kClass.nestedClasses
    val nestedClass = nestedClasses.single()
    return nestedClass.simpleName!!
}


// FILE: Script.kts

class OK
typealias Tazz = List<OK>
val x: Tazz = listOf()
x
