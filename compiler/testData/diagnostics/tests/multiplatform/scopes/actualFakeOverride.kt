// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>HashMap<!> {
    val size: Int
}

expect abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>AbstractMap<!> {
    val size: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual abstract class AbstractMap() {
    actual val size: Int = 0
}

actual class HashMap : AbstractMap()
