// TARGET_BACKEND: JVM_IR
// WITH_REFLECT

annotation class A(vararg val strings: String)

@A(*arrayOf("foo", "bar"), "baz")
class B

@A("baz", *arrayOf("foo", "bar"), "xyz")
class C

@A(*arrayOf("foo", "bar", "xyz"))
class D

@A("foo", "baz", "bar")
class E

@A(*arrayOf("foo", "bar"), *arrayOf("baz", "xyz"))
class F

fun box(): String {
    assert((B::class.annotations.single() as A).strings.contentEquals(arrayOf("foo", "bar", "baz"))) { "Fail1" }
    assert((C::class.annotations.single() as A).strings.contentEquals(arrayOf("baz", "foo", "bar", "xyz"))) { "Fail 2" }
    assert((D::class.annotations.single() as A).strings.contentEquals(arrayOf("foo", "bar", "xyz"))) { "Fail 3" }
    assert((E::class.annotations.single() as A).strings.contentEquals(arrayOf("foo", "baz", "bar"))) { "Fail 4" }
    assert((F::class.annotations.single() as A).strings.contentEquals(arrayOf("foo", "bar", "baz", "xyz"))) { "Fail 5" }
    return "OK"
}
