// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS

package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

class C<A, B>

class Kotlin {

    fun foo(s: C<@TypeAnn("1") String, @TypeAnn("2") Int>) {
    }


    fun fooVariance(s: C<in @TypeAnn("3") String, out @TypeAnn("4") Int>) {
    }

}
