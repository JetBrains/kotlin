// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// See KT-54823

@RequiresOptIn
annotation class Marker

interface VeryBase<T> {
    fun foo(base: VeryBase<*>) {}
}

@Marker
abstract class Base<T> : VeryBase<T> {
    fun bar(base: Base<*>) {}

    fun baz() {}
}

@OptIn(Marker::class)
open class Intermediate : Base<String>()

class Derived : Intermediate()

fun main() {
    val d = Derived()
    // Should be Ok (declared in VeryBase without marker)
    d.foo(d)
    // Should be Ok (declared in Base with marker, but called on a receiver of type Derived without marker)
    d.baz()
    // Should be Error (has a parameter of type Base with marker)
    d.<!OPT_IN_USAGE_ERROR!>bar<!>(d)
}
