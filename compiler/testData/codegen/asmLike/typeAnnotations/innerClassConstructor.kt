// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

class Kotlin {
    inner class Inner(s: @TypeAnn String) {}
}
