// IGNORE_BACKEND: JVM
// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
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
