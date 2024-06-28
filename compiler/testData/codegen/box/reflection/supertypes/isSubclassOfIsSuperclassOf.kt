// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.test.assertTrue
import kotlin.test.assertFalse

open class Klass
interface Interface<T>
class Bar : Interface<String>, Klass()

fun check(subclass: KClass<*>, superclass: KClass<*>, shouldBeSubclass: Boolean) {
    if (shouldBeSubclass) {
        assertTrue(subclass.isSubclassOf(superclass))
        assertTrue(superclass.isSuperclassOf(subclass))
    } else {
        assertFalse(subclass.isSubclassOf(superclass))
        assertFalse(superclass.isSuperclassOf(subclass))
    }
}

fun box(): String {
    check(Any::class, Any::class, true)
    check(String::class, Any::class, true)
    check(Any::class, String::class, false)
    check(String::class, String::class, true)

    check(Int::class, Int::class, true)
    check(Int::class, Any::class, true)

    check(List::class, Collection::class, true)
    check(List::class, Iterable::class, true)
    check(Collection::class, Iterable::class, true)
    check(Set::class, List::class, false)

    check(Array<String>::class, Array<Any>::class, false)
    check(Array<Any>::class, Array<String>::class, false)

    check(Function3::class, Function4::class, false)
    check(Function4::class, Function3::class, false)

    check(Bar::class, Klass::class, true)
    check(Bar::class, Interface::class, true)
    check(Klass::class, Bar::class, false)
    check(Interface::class, Bar::class, false)
    check(Klass::class, Interface::class, false)

    return "OK"
}
