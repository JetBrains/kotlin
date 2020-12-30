fun f() {
    <!CAN_BE_VAL{LT}, REDUNDANT_VISIBILITY_MODIFIER{LT}, UNUSED_VARIABLE{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>public<!> <!CAN_BE_VAL{PSI}!>var<!> <!UNUSED_VARIABLE{PSI}!>baz<!> = 0<!>
    class LocalClass {
        <!REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>internal<!> var foo = 0<!>
    }
    LocalClass().foo = 1
}

internal inline fun internal() {
    f()
}

<!REDECLARATION!>class C {
    internal val z = object {
        fun foo() = 13
    }
}<!>

class Foo2<
        T1,
        T2: T1,
        > {
    fun <T1,
            T2, > foo2() {}

    internal inner class B<T,T2,>
}

<!REDECLARATION, REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>public<!> class C {
    <!REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>public<!> val foo: Int = 0<!>

    <!REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>public<!> fun bar() {}<!>

}<!>

open class D {
    protected open fun willRemainProtected() {
    }

    protected open fun willBecomePublic() {
    }
}

class E : D() {
    <!REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>protected<!> override fun willRemainProtected() {
    }<!>

    public override fun willBecomePublic() {
    }
}

enum class F <!REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>private<!> constructor(val x: Int)<!> {
    FIRST(42)
}

sealed class G constructor(val y: Int) {
    <!REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>private<!> constructor(): this(42)<!>

    object H : G()
}

interface I {
    fun bar()
}

<!REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>public<!> var baz = 0<!>

open class J {
    protected val baz = 0
    <!REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>protected<!> get() = field * 2<!>
    var baf = 0
    <!REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>public<!> get() = 1<!>
    <!REDUNDANT_VISIBILITY_MODIFIER{LT}!><!REDUNDANT_VISIBILITY_MODIFIER{PSI}!>public<!> set(value) {
        field = value
    }<!>

    var buf = 0
        private get() = 42
        protected set(value) {
            field = value
        }

    var bar = 0
        get() = 3.1415926535
        set(value) {}
}
