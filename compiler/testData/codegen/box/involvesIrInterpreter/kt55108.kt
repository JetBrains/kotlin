// TARGET_BACKEND: JVM_IR
// WITH_REFLECT

annotation class A(vararg val strings: String)

@A(*arrayOf(<!EVALUATED("foo")!>"foo"<!>, <!EVALUATED("bar")!>"bar"<!>), <!EVALUATED("baz")!>"baz"<!>)
class B

@A(<!EVALUATED("baz")!>"baz"<!>, *arrayOf(<!EVALUATED("foo")!>"foo"<!>, <!EVALUATED("bar")!>"bar"<!>), <!EVALUATED("xyz")!>"xyz"<!>)
class C

@A(*arrayOf(<!EVALUATED("foo")!>"foo"<!>, <!EVALUATED("bar")!>"bar"<!>, <!EVALUATED("xyz")!>"xyz"<!>))
class D

@A(<!EVALUATED("foo")!>"foo"<!>, <!EVALUATED("baz")!>"baz"<!>, <!EVALUATED("bar")!>"bar"<!>)
class E

@A(*arrayOf(<!EVALUATED("foo")!>"foo"<!>, <!EVALUATED("bar")!>"bar"<!>), *arrayOf(<!EVALUATED("baz")!>"baz"<!>, <!EVALUATED("xyz")!>"xyz"<!>))
class F

fun box(): String {
    assert((B::class.annotations.single() as A).strings.contentEquals(arrayOf("foo", "bar", "baz"))) { "Fail1" }
    assert((C::class.annotations.single() as A).strings.contentEquals(arrayOf("baz", "foo", "bar", "xyz"))) { "Fail 2" }
    assert((D::class.annotations.single() as A).strings.contentEquals(arrayOf("foo", "bar", "xyz"))) { "Fail 3" }
    assert((E::class.annotations.single() as A).strings.contentEquals(arrayOf("foo", "baz", "bar"))) { "Fail 4" }
    assert((F::class.annotations.single() as A).strings.contentEquals(arrayOf("foo", "bar", "baz", "xyz"))) { "Fail 5" }
    return "OK"
}
