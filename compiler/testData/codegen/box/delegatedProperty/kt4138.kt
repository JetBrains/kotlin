class Delegate<T>(var inner: T) {
    fun get(t: Any?, p: PropertyMetadata): T = inner
    fun set(t: Any?, p: PropertyMetadata, i: T) { inner = i }
}


class Foo (val f: Int) {
    default object {
        val A: Foo by Delegate(Foo(11))
        var B: Foo by Delegate(Foo(11))
    }
}

trait FooTrait {
    default object {
        val A: Foo by Delegate(Foo(11))
        var B: Foo by Delegate(Foo(11))
    }
}

fun box() : String {
    if (Foo.A.f != 11) return "fail 1"
    if (Foo.B.f != 11) return "fail 2"

    Foo.B = Foo(12)
    if (Foo.B.f != 12) return "fail 3"

    if (FooTrait.A.f != 11) return "fail 4"
    if (FooTrait.B.f != 11) return "fail 5"

    FooTrait.B = Foo(12)
    if (FooTrait.B.f != 12) return "fail 6"

    return "OK"
}