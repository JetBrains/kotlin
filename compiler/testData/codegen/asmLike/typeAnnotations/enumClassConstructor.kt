// IGNORE_BACKEND: JVM_IR
// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// TYPE_ANNOTATIONS
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
package foo


@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

enum class Kotlin (s: @TypeAnn String) {
    A("123") {
        fun foo() {}
    };
}
