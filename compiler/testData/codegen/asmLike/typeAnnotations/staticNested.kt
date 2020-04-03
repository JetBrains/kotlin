// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// TYPE_ANNOTATIONS
// IGNORE_BACKEND_FIR: JVM_IR
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

class Outer {
    class NestedStatic<T>
}

class Kotlin {

    fun foo(s: @Ann Outer.NestedStatic<@Ann2 String>) {
    }

    fun foo(): @Ann Outer.NestedStatic<@Ann2 String>? {
        return null
    }

    fun fooArray(s: @Ann Array<@Ann2 Outer.NestedStatic<@Ann3 String>>) {
    }

    fun fooArray(): @Ann Array<@Ann2 Outer.NestedStatic<@Ann3 String>>? {
        return null
    }

    fun fooArrayIn(s: @Ann Array<in @Ann2 Outer.NestedStatic<@Ann3 String>>) {
    }

    fun fooArrayOut(): @Ann Array<out @Ann2 Outer.NestedStatic<@Ann3 String>>? {
        return null
    }


    fun <T> fooGenericIn(s: @Ann Bar<in @Ann2 T>) {
    }

    fun <T> fooGenericOut(s: @Ann Bar<out @Ann2 T>) {
    }


}
