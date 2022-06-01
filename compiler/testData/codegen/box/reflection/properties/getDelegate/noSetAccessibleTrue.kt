// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KProperty
import kotlin.reflect.KProperty2
import kotlin.reflect.full.IllegalPropertyDelegateAccessException
import kotlin.test.*

object Delegate {
    operator fun getValue(instance: Any?, property: KProperty<*>) = true
}

val topLevel: Boolean by Delegate
val delegated: Boolean by ::topLevel
val String.extension: Boolean by Delegate
val String.delegated: Boolean by String::delegated

class Foo {
    val member: Boolean by Delegate
    val delegated: Boolean by ::member
    val String.memberExtension: Boolean by Delegate
    val String.memberExtensionDelegated: Boolean by ::member
}

inline fun check(block: () -> Unit) {
    try {
        block()
        throw AssertionError("No IllegalPropertyDelegateAccessException has been thrown")
    } catch (e: IllegalPropertyDelegateAccessException) {
        // OK
    }
}

fun box(): String {
    check { ::topLevel.getDelegate() }
    check { ::delegated.getDelegate() }

    check { String::extension.getDelegate("") }
    check { ""::extension.getDelegate() }
    check { String::delegated.getDelegate("") }
    check { ""::delegated.getDelegate() }

    val foo = Foo()
    check { Foo::member.getDelegate(foo) }
    check { foo::member.getDelegate() }
    check { Foo::delegated.getDelegate(foo) }
    check { foo::delegated.getDelegate() }

    val me = Foo::class.members.single { it.name == "memberExtension" } as KProperty2<Foo, String, Boolean>
    check { me.getDelegate(foo, "") }

    val med = Foo::class.members.single { it.name == "memberExtensionDelegated" } as KProperty2<Foo, String, Boolean>
    check { med.getDelegate(foo, "") }

    return "OK"
}
