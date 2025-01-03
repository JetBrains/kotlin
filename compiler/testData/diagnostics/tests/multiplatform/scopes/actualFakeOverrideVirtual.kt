// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Map<!> {
    val size: Int
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>HashMap<!> : Map {
    override val size: Int
}

expect abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>AbstractMap<!> : Map {
    override val size: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual abstract class AbstractMap() : Map {
    actual override val size: Int = 0
}

actual class HashMap : AbstractMap(), Map
