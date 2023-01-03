// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

class A {
    @nativeSetter
    fun set(a: String, v: Any?): Any? = null

    @nativeSetter
    fun put(a: Number, v: String) {}

    @nativeSetter
    fun foo(a: Int, v: String) {}

    companion object {
        @nativeSetter
        fun set(a: String, v: Any?): Any? = null

        @nativeSetter
        fun put(a: Number, v: String) {}

        @nativeSetter
        fun foo(a: Int, v: String) {}
    }
}

class B {
    <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
    val foo = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
    object Obj1 {}

    companion object {
        <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
        val foo = 0

        <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
        object Obj2 {}
    }
}

class C {
    @nativeSetter
    fun Int.set(a: String, v: Int) {}

    @nativeSetter
    fun Int.set2(a: Number, v: String?) = "OK"

    @nativeSetter
    fun Int.set3(a: Double, v: String?) = "OK"

    @nativeSetter
    fun set(): Any? = null

    @nativeSetter
    fun set(a: A): Any? = null

    @nativeSetter
    fun set(a: String, v: Any, v2: Any) {}

    @nativeSetter
    fun set(a: A, v: Any?) {}

    @nativeSetter
    fun foo(a: String = "0.0", v: String) = "OK"
}
