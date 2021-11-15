// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
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
