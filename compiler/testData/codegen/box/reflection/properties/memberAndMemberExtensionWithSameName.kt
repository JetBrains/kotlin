// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.*

class A {
    val foo: String = "member"
    val Unit.foo: String get() = "extension"
}

fun box(): String {
    run {
        val foo: KProperty1<A, *> = A::class.java.kotlin.memberProperties.single()
        assert(foo.name == "foo") { "Fail name: $foo (${foo.name})" }
        assert(foo.get(A()) == "member") { "Fail value: ${foo.get(A())}" }
    }

    run {
        val foo: KProperty2<A, *, *> = A::class.java.kotlin.memberExtensionProperties.single()
        assert(foo.name == "foo") { "Fail name: $foo (${foo.name})" }
        foo as KProperty2<A, Unit, *>
        assert(foo.get(A(), Unit) == "extension") { "Fail value: ${foo.get(A(), Unit)}" }
    }

    return "OK"
}
