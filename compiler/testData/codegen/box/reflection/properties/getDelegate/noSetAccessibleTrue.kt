// IGNORE_BACKEND_FIR: JVM_IR
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
val String.extension: Boolean by Delegate

class Foo {
    val member: Boolean by Delegate
    val String.memberExtension: Boolean by Delegate
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

    check { String::extension.getDelegate("") }
    check { ""::extension.getDelegate() }

    val foo = Foo()
    check { Foo::member.getDelegate(foo) }
    check { foo::member.getDelegate() }

    val me = Foo::class.members.single { it.name == "memberExtension" } as KProperty2<Foo, String, Boolean>
    check { me.getDelegate(foo, "") }

    return "OK"
}
