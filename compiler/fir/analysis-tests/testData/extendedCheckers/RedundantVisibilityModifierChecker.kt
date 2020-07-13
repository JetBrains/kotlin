<!REDUNDANT_VISIBILITY_MODIFIER!>public<!> class C {
    <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> val foo: Int = 0

    <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> fun bar() {}

}

open class D {
    protected open fun willRemainProtected() {
    }

    protected open fun willBecomePublic() {
    }
}

class E : D() {
    <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> override fun willRemainProtected() {
    }

    public override fun willBecomePublic() {
    }
}

enum class F <!REDUNDANT_VISIBILITY_MODIFIER!>private<!> constructor(val x: Int) {
    FIRST(42)
}

sealed class G constructor(val y: Int) {
    <!REDUNDANT_VISIBILITY_MODIFIER!>private<!> constructor(): this(42)

    object H : G()
}

interface I {
    fun bar()
}

fun f() {
    val i = object : I {
        <!REDUNDANT_VISIBILITY_MODIFIER!>internal<!> var foo = 0
        override fun bar() {}
    }
    i.foo = 1

    class LocalClass {
        <!REDUNDANT_VISIBILITY_MODIFIER!>internal<!> var foo = 0
    }
    LocalClass().foo = 1
    <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> var baz = 0
}

<!REDUNDANT_VISIBILITY_MODIFIER!>public<!> var baz = 0

open class J {
    protected val baz = 0
    <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> get() = field * 2
    var baf = 0
    <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> get() = 1
    <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> set(value) {
        field = value
    }

    var buf = 0
    private get() = 42
    protected set(value) {
        field = value
    }

    var bar = 0
    get() = 3.1415926535
    set(value) {}
}
