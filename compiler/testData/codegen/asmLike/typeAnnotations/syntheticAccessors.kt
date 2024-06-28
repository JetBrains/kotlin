// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS

package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnBinary

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeAnnSource

class Kotlin {

    private fun foo(s: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource String): @TypeAnn("2") @TypeAnnBinary @TypeAnnSource String {
        return "OK"
    }

    inner class A {

        fun fooArray2() {
            foo("123")
        }
    }

}
