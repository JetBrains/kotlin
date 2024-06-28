// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS

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
    inner class Inner<T>
}

class Kotlin {

    fun foo(s: @Ann Outer.Inner<@Ann2 String>) {
    }

    fun foo(): @Ann Outer.Inner<@Ann2 String>? {
        return null
    }

    fun fooArray(s: @Ann Array<@Ann2 Outer.Inner<@Ann3 String>>) {
    }

    fun fooArray(): @Ann Array<@Ann2 Outer.Inner<@Ann3 String>>? {
        return null
    }

    fun fooArrayIn(s: @Ann Array<in @Ann2 Outer.Inner<@Ann3 String>>) {
    }

    fun fooArrayOut(): @Ann Array<out @Ann2 Outer.Inner<@Ann3 String>>? {
        return null
    }

    fun <T: Bar<Outer.Inner<@Ann Outer>>> innerClassInBound() {
    }
}

class WithBound<T: Bar<Outer.Inner<@Ann Outer>>>()
