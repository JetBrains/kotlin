import kotlin.reflect.jvm.kotlin
import kotlin.reflect.KMemberProperty
import kotlin.reflect.KMemberExtensionProperty

class A {
    val foo: String = "member"
    val Unit.foo: String get() = "extension"
}

fun box(): String {
    run {
        val foo: KMemberProperty<A, *> = javaClass<A>().kotlin.getProperties().single()
        assert(foo.name == "foo") { "Fail name: $foo (${foo.name})" }
        assert(foo.get(A()) == "member") { "Fail value: ${foo[A()]}" }
    }

    run {
        val foo: KMemberExtensionProperty<A, *, *> = javaClass<A>().kotlin.getExtensionProperties().single()
        assert(foo.name == "foo") { "Fail name: $foo (${foo.name})" }
        foo as KMemberExtensionProperty<A, Unit, *>
        assert(foo.get(A(), Unit) == "extension") { "Fail value: ${foo[A(), Unit]}" }
    }

    return "OK"
}
