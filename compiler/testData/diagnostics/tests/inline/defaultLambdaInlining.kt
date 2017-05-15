// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
// !LANGUAGE: +InlineDefaultFunctionalParameters

fun test() = "OK"

val prop = "OK"

class Foo {
    fun test() = "OK"
    val prop = "OK"
}

object FooObject {
    fun test() = "OK"
    val prop = "OK"
}

inline fun default1(s : () -> String = {"OK"}) {}
inline fun default2(s : () -> String = ::test) {}
inline fun default3(s : () -> String = ::prop) {}
inline fun default4(s : () -> String = FooObject::test) {}
inline fun default5(s : () -> String = FooObject::prop) {}
inline fun default6(s : (a: Foo) -> String = Foo::test) {}
inline fun default7(s : (a: Foo) -> String = Foo::prop) {}

val a = Foo()

inline fun default8(s : () -> String = a::test) {}
inline fun default9(s : () -> String = a::prop) {}

inline fun default10(s : () -> String = <!INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE!>object : Function0<String><!> {
    override fun invoke(): String {
        return "FAIL"
    }
}) {}

abstract class Base {
    abstract fun foo(f: () -> Unit = { })
}

class Derived : Base() {
    <!OVERRIDE_BY_INLINE!>override final inline fun foo(<!NOT_YET_SUPPORTED_IN_INLINE!>f: () -> Unit<!>)<!> {
        f()
    }
}

inline fun default11(s : () -> Derived = ::Derived) {}