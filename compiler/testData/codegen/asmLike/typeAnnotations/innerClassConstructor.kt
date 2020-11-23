// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// TYPE_ANNOTATIONS
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

class Kotlin {
    inner class Inner(s: @TypeAnn String) {}
}