// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
package foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

@Target(AnnotationTarget.TYPE)
annotation class Ann2

@Target(AnnotationTarget.TYPE)
annotation class Ann3

class Inv<T>
class Out<out T>
class In<in T>

class Kotlin {
    fun <T> star(x: @Ann Inv<*>) {}
    fun <T> invUse(x: @Ann Inv<@Ann2 Inv<@Ann3 T>>) {}
    fun <T> outUse(x: @Ann Inv<out @Ann2 Inv<@Ann3 T>>) {}
    fun <T> outDeclaration(x: @Ann Out<@Ann2 Inv<@Ann3 T>>) {}
    fun <T> inUse(x: @Ann Inv<in @Ann2 Inv<@Ann3 T>>) {}
    fun <T> inDeclaration(x: @Ann In<@Ann2 Inv<@Ann3 T>>) {}

    fun <T> mappedCollection(x: @Ann List<@Ann2 Inv<@Ann3 T>>) {}
}
