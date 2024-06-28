// FIR_IDENTICAL
// ENABLE_EXPECT_ACTUAL_CLASSES_WARNING
// LANGUAGE: +ExpectActualClasses
// MODULE: m1-common
// FILE: common.kt

expect class Clazz {
    class Nested

    fun memberFun()
    val memberProp: Clazz
}

expect interface Interface

expect object Object

expect annotation class Annotation

expect enum class Enum

expect class ActualTypealias

expect fun function()

expect val property: Clazz

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Clazz {
    actual class Nested

    actual fun memberFun() {}
    actual val memberProp: Clazz = null!!
}

actual interface Interface

actual object Object

actual annotation class Annotation

actual enum class Enum

actual typealias ActualTypealias = ActualTypealiasImpl

class ActualTypealiasImpl

actual fun function() {}

actual val property: Clazz = null!!
