// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !LANGUAGE: +OptInOnOverrideForbidden
// FILE: api.kt

package api

import kotlin.annotation.AnnotationTarget.*

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION,
        PROPERTY_SETTER, TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
annotation class E1

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(FILE)<!>
annotation class E2

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(EXPRESSION)<!>
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_RETENTION!>@Retention(AnnotationRetention.SOURCE)<!>
annotation class E3

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(TYPE_PARAMETER)<!>
@Retention(AnnotationRetention.BINARY)
annotation class E3A

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
<!EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET!>@Target(TYPE)<!>
@Retention(AnnotationRetention.BINARY)
annotation class E3B

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
annotation class E4

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
annotation class E5

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(PROPERTY, FUNCTION, PROPERTY_SETTER, VALUE_PARAMETER, FIELD, LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
annotation class E6

var some: Int
    @E4
    get() = 42
    @E5
    set(value) {}

@get:E4
val another: Int = 42

class My {
    @E6
    override fun hashCode() = 0
}

interface Base {
    val bar: Int

    val baz: Int

    @E6
    fun foo()

    fun String.withReceiver()
}

class Derived : Base {
    @E6
    override val bar: Int = 42

    @set:E6 @setparam:E6
    override var baz: Int = 13

    @E6
    override fun foo() {}

    override fun @receiver:E6 String.withReceiver() {}
}

abstract class Another(@param:E6 val x: String) : Base {
    @delegate:E6
    override val bar: Int by lazy { 42 }

    fun baz(@E6 param: Int) {
        @E6 val x = param
    }
}

interface A {
    fun f()
}

interface B {
    @E6
    fun f()
}

interface C1 : A, B
interface C2 : B, A

class X1 : C1 {
    @E6 // Ok
    override fun f() {}
}

class X2 : C2 {
    @E6 // Ok
    override fun f() {}
}

open class Y(val b: B): B by b

class Z(b: B) : Y(b) {
    @E6
    override fun f() {}
}