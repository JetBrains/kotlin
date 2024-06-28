// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*

class A {
    val foo: String = "member"
    val Unit.foo: String get() = "extension"
}

fun box(): String {
    run {
        val foo: KProperty1<A, *> = A::class.memberProperties.single()
        assert(foo.name == "foo") { "Fail name: $foo (${foo.name})" }
        assert(foo.get(A()) == "member") { "Fail value: ${foo.get(A())}" }
    }

    run {
        val foo: KProperty2<A, *, *> = A::class.memberExtensionProperties.single()
        assert(foo.name == "foo") { "Fail name: $foo (${foo.name})" }
        foo as KProperty2<A, Unit, *>
        assert(foo.get(A(), Unit) == "extension") { "Fail value: ${foo.get(A(), Unit)}" }
    }

    return "OK"
}
