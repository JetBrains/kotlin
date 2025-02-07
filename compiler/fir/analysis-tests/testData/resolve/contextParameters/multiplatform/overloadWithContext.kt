// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
expect class Test1 {
    fun foo()
}

expect class Test2 {
    context(a: Int)
    fun foo()
}

expect class Test3 {
    val a: String
}

expect class Test4 {
    context(a: Int)
    val a: String
}

expect class Test5 {
    fun foo(a: Int): String
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Test1 {
    actual fun foo() {}

    context(a: Int)
    fun foo() {}
}

actual class Test2 {
    context(a: Int)
    actual fun foo() {}

    fun foo() {}
}

actual class Test3 {
    actual val a: String = ""

    context(a: Int)
    val a: String
        get() = ""
}

actual class Test4 {
    context(a: Int)
    actual val a: String
        get() = ""

    val a: String = ""
}

actual class Test5 {
    actual fun foo(a: Int): String { return "" }

    context(a: Int)
    val foo
        get() = { }
}