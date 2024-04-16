// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// WITH_STDLIB

package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

class FooClass {
    @JvmOverloads
    fun foo(s: @TypeAnn("1") String = "1", x: @TypeAnn("2") Int = 123) : @TypeAnn("return") Any? {
        return null
    }
}
