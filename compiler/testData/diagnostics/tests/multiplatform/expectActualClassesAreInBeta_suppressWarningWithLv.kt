// RUN_PIPELINE_TILL: BACKEND
// ENABLE_EXPECT_ACTUAL_CLASSES_WARNING
// LANGUAGE: +ExpectActualClasses
// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Clazz<!> {
    class Nested

    fun memberFun()
    val memberProp: Clazz
}

expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Interface<!>

expect object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Object<!>

expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Annotation<!>

expect enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Enum<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ActualTypealias<!>

<!CONFLICTING_OVERLOADS!>expect fun function()<!>

expect val <!REDECLARATION!>property<!>: Clazz

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
