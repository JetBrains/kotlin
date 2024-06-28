// LANGUAGE: +MultiPlatformProjects
// IGNORE_LIGHT_TREE
// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K2: JVM_IR
// MODULE: common
// FILE: common.kt

expect value class Wrapper(val obj: Any) {
    val prop: String // [PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS] Value class cannot have properties with backing fields
}

// MODULE: main()()(common)
// FILE: test.kt

actual value class Wrapper(val obj: Any) {
    actual val prop: String
        get() = "OK"
}

fun box(): String = Wrapper("").prop
