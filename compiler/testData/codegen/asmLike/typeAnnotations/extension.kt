// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS

package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

class Kotlin {

    fun @TypeAnn("ext") String.foo2(s: @TypeAnn("param") String) {
    }
}
