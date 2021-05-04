// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
package foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

@Target(AnnotationTarget.TYPE)
annotation class Ann2

@Target(AnnotationTarget.TYPE)
annotation class Ann3

@Target(AnnotationTarget.TYPE)
annotation class Ann4

class Bar<T>

class Kotlin {

    fun foo(s: @Ann Bar<@Ann2 String>) {
    }

    fun foo(): @Ann Bar<@Ann2 String>? {
        return null
    }

    fun fooArray(s: @Ann Array<@Ann2 Bar<@Ann3 String>>) {
    }

    fun fooArray(): @Ann Array<@Ann2 Bar<@Ann3 String>>? {
        return null
    }

    fun fooArrayArray(s: @Ann Array<@Ann2 Array<@Ann3 Bar<@Ann4 String>>>) {
    }

    fun fooArrayArray(): @Ann Array<@Ann2 Array<@Ann3 Bar<@Ann4 String>>>? {
        return null
    }


    fun <T> foo(s: @Ann T) {
    }

    fun <T> fooGeneric(s: @Ann Bar<@Ann2 T>) {
    }
}
