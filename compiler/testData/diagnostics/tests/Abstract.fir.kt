// FILE: b.kt
package MyPackage
    //properties
    <!MUST_BE_INITIALIZED!>val a: Int<!>
    val a1: Int = 1
    abstract val a2: Int
    abstract val a3: Int = 1

    <!MUST_BE_INITIALIZED!>var b: Int<!>                private set
    var b1: Int = 0;                         private set
    abstract var b2: Int      private set
    abstract var b3: Int = 0; private set

    <!MUST_BE_INITIALIZED!>var c: Int<!>                set(v: Int) { field = v }
    var c1: Int = 0;                         set(v: Int) { field = v }
    abstract var c2: Int      set(v: Int) { field = v }
    abstract var c3: Int = 0; set(v: Int) { field = v }

    val e: Int                               get() = a
    val e1: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>;          get() = a
    abstract val e2: Int      get() = a
    abstract val e3: Int = 0; get() = a

    //methods
    <!NON_MEMBER_FUNCTION_NO_BODY!>fun f()<!>
    fun g() {}
    abstract fun h()
    abstract fun j() {}

    //property accessors
    <!MUST_BE_INITIALIZED!>var i: Int<!>                       abstract get  abstract set
    var i1: Int = 0;  abstract get  abstract set

    <!MUST_BE_INITIALIZED!>var j: Int<!>                       get() = i;    abstract set
    var j1: Int = 0;  get() = i;    abstract set

    <!MUST_BE_INITIALIZED!>var k: Int<!>        abstract set
    var k1: Int = 0;                 abstract set

    <!MUST_BE_INITIALIZED!>var l: Int<!>                       abstract get  abstract set
    var l1: Int = 0;  abstract get  abstract set

    <!MUST_BE_INITIALIZED!>var n: Int<!>                       abstract get abstract set(v: Int) {}

// FILE: c.kt
//creating an instance
abstract class B1(
    val i: Int,
    val s: String
) {
}

class B2() : B1(1, "r") {}

abstract class B3(i: Int) {
}

fun foo(c: B3) {
    val a = B3(1)
    val b = B1(2, "s")
}