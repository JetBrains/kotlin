// LANGUAGE: +MultiPlatformProjects
// IGNORE_LIGHT_TREE
// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: common
// FILE: common.kt

expect value class Wrapper(val obj: Any) {
    val prop: String // [PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS] Value class cannot have properties with backing fields
}

// MODULE: main()()(common)
// FILE: test.kt

@JvmInline
actual value class Wrapper(val obj: Any) {
    actual val prop: String
        get() = "OK"
}

fun box(): String = Wrapper("").prop
