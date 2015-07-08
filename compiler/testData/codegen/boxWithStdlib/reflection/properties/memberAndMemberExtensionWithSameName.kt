import kotlin.reflect.jvm.kotlin
import kotlin.reflect.*

class A {
    val foo: String = "member"
    val Unit.foo: String get() = "extension"
}

fun box(): String {
    run {
        val foo: KProperty1<A, *> = javaClass<A>().kotlin.properties.single()
        assert(foo.name == "foo") { "Fail name: $foo (${foo.name})" }
        assert(foo.get(A()) == "member") { "Fail value: ${foo[A()]}" }
    }

    run {
        val foo: KProperty2<A, *, *> = javaClass<A>().kotlin.extensionProperties.single()
        assert(foo.name == "foo") { "Fail name: $foo (${foo.name})" }
        foo as KProperty2<A, Unit, *>
        assert(foo.get(A(), Unit) == "extension") { "Fail value: ${foo[A(), Unit]}" }
    }

    return "OK"
}
