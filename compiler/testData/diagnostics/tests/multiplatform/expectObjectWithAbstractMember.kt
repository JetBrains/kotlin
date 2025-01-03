// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!> {
    fun foo()
}

expect object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Implementation<!> : Base

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

abstract class RealImplementation : Base {
    override fun foo() {}
}

actual object Implementation : RealImplementation(), Base
