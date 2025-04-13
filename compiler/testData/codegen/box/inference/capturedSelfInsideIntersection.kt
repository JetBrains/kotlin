// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ISSUE: KT-74999
// WITH_REFLECT
// DUMP_IR

import kotlin.reflect.KClass

interface GraphQlTester {
    interface Traversable {}
    interface Entity<D, S : Entity<D, S>> : Traversable {
        fun <T : S> isEqualTo(expected: Any?): T
    }

    interface Path : Traversable {
        fun valueIsNull(): Path
        fun <E : Any> entity(entityType: KClass<E>): GraphQlTester.Entity<E, *>
    }
}

inline fun <reified U : Any> GraphQlTester.Path.isEqualTo(expected: U?) {
    if (null == expected) valueIsNull()
    // Type parameter T of isEqualTo shouldn't be inferred to Nothing
    else entity(U::class).isEqualTo(expected)
}

open class EntityImpl<D> : GraphQlTester.Entity<D, EntityImpl<D>> {
    override fun <T : EntityImpl<D>> isEqualTo(expected: Any?): T = this as T
}

class PathImpl : GraphQlTester.Path {
    override fun valueIsNull() = this

    override fun <E : Any> entity(entityType: KClass<E>): GraphQlTester.Entity<E, *> =
        EntityImpl<E>()
}

fun box(): String {
    PathImpl().isEqualTo(42)
    return "OK"
}
