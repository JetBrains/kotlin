// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

class FooClass {

    companion object {
        @JvmStatic
        fun foo(s: @TypeAnn("1") String, x: @TypeAnn("2") Int): @TypeAnn("return") Any? {
            return null
        }
    }

}

object Foo {
    @JvmStatic
    fun foo(s: @TypeAnn("1") String , x: @TypeAnn("2") Int): @TypeAnn("return") Any? {
        return null
    }
}
