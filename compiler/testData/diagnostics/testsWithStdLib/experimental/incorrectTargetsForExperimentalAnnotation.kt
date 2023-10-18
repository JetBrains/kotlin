// FIR_IDENTICAL
// !OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -POTENTIALLY_NON_REPORTED_ANNOTATION
// FILE: api.kt

package api

import kotlin.annotation.AnnotationTarget.*

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION,
        PROPERTY_SETTER, TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
annotation class E1

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
<!OPT_IN_MARKER_WITH_WRONG_TARGET!>@Target(FILE)<!>
annotation class E2

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
<!OPT_IN_MARKER_WITH_WRONG_TARGET!>@Target(EXPRESSION)<!>
<!OPT_IN_MARKER_WITH_WRONG_RETENTION!>@Retention(AnnotationRetention.SOURCE)<!>
annotation class E3

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
<!OPT_IN_MARKER_WITH_WRONG_TARGET!>@Target(TYPE_PARAMETER)<!>
@Retention(AnnotationRetention.BINARY)
annotation class E3A

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
<!OPT_IN_MARKER_WITH_WRONG_TARGET!>@Target(TYPE)<!>
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
@Target(PROPERTY, FUNCTION, PROPERTY_SETTER, VALUE_PARAMETER, FIELD, LOCAL_VARIABLE, CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class E6

var some: Int
    <!OPT_IN_MARKER_ON_WRONG_TARGET!>@E4<!>
    get() = 42
    @E5
    set(value) {}

<!OPT_IN_MARKER_ON_WRONG_TARGET!>@get:E4<!>
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

    @set:E6 <!OPT_IN_MARKER_ON_WRONG_TARGET!>@setparam:E6<!>
    override var baz: Int = 13

    @E6
    override fun foo() {}

    override fun <!OPT_IN_MARKER_ON_WRONG_TARGET!>@receiver:E6<!> String.withReceiver() {}
}

class Wrapper(@property:E6 val foo: Int)

@E6
interface BaseMarked {
    val bar: Int
}

@E6
class Outer {
    interface Nested {
        val baz: Int
    }
}

@OptIn(E6::class)
class DerivedOptIn : BaseMarked, Outer.Nested {
    @E6
    override val bar: Int = 42 // Ok

    @E6
    override val baz: Int = 24 // Ok
}

abstract class Another(<!OPT_IN_MARKER_ON_WRONG_TARGET!>@param:E6<!> val x: String) : Base {
    <!OPT_IN_MARKER_ON_WRONG_TARGET!>@delegate:E6<!>
    override val bar: Int by lazy { 42 }

    fun baz(<!OPT_IN_MARKER_ON_WRONG_TARGET!>@E6<!> param: Int) {
        <!OPT_IN_MARKER_ON_WRONG_TARGET!>@E6<!> val x = param
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

class WithSetter(@set:E6 var withSetter: String)
