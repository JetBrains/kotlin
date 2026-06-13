// common.pack.ClassToCheck
// MODULE: main-common
// FILE: common.kt
package common.pack

expect class ClassToCheck

// MODULE: m1-jvm()()(main-common)
// FILE: jvm.kt
package common.pack

actual class ClassToCheck {
    fun foo() {}
}
