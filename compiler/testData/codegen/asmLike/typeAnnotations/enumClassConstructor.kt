// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS

package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

enum class Kotlin (s: @TypeAnn String) {
    A("123") {
        fun foo() {}
    };
}
