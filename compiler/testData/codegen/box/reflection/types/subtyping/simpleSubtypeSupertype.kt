// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.full.*
import kotlin.test.assertTrue
import kotlin.test.assertFalse

fun check(subCallable: KCallable<*>, superCallable: KCallable<*>, shouldBeSubtype: Boolean) {
    val subtype = subCallable.returnType
    val supertype = superCallable.returnType
    if (shouldBeSubtype) {
        assertTrue(subtype.isSubtypeOf(supertype))
        assertTrue(supertype.isSupertypeOf(subtype))
    } else {
        assertFalse(subtype.isSubtypeOf(supertype))
        assertFalse(supertype.isSupertypeOf(subtype))
    }
}

open class O
class X : O()

fun any(): Any = null!!
fun string(): String = null!!
fun nullableString(): String? = null!!
fun int(): Int = null!!
fun nothing(): Nothing = null!!
fun nullableNothing(): Nothing? = null!!
fun function2(): (Any, Any) -> Any = null!!
fun function3(): (Any, Any, Any) -> Any = null!!

fun box(): String {
    check(::any, ::any, true)
    check(::int, ::int, true)
    check(::nothing, ::nothing, true)
    check(::nullableNothing, ::nullableNothing, true)

    check(::string, ::any, true)
    check(::nullableString, ::any, false)
    check(::int, ::any, true)
    check(::O, ::any, true)
    check(::X, ::any, true)

    check(::nothing, ::any, true)
    check(::nothing, ::string, true)
    check(::nothing, ::nullableString, true)
    check(::nullableNothing, ::nullableString, true)
    check(::nullableNothing, ::string, false)

    check(::string, ::nullableString, true)
    check(::nullableString, ::string, false)

    check(::X, ::O, true)
    check(::O, ::X, false)

    check(::int, ::string, false)
    check(::string, ::int, false)
    check(::any, ::string, false)
    check(::any, ::nullableString, false)

    check(::function2, ::function3, false)
    check(::function3, ::function2, false)

    return "OK"
}
