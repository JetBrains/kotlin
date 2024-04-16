// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS

package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

class Kotlin {
    inner class Inner(s: @TypeAnn String) {}
}
