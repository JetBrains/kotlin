// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.*

annotation class Ann(
        val kc1: KClass<*>,
        val kc2: KClass<*>,
        val kc3: KClass<*>,
        val c: Int,
        val cc: Int,
        val cn: Int,
        val ci: Int,
        val t1: Int,
        val t2: Int
)

@Ann(
        Nested::class,
        Inner::class,
        Interface::class,
        CONST,
        Companion.CONST,
        Nested.CONST,
        Interface.CONST,
        <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>a<!>,
        b()
)
class A {

    class Nested {
        companion object {
            const val CONST = 2
        }
    }

    inner class Inner

    interface Interface {
        companion object {
            const val CONST = 3
        }
    }

    val a = 1
    fun b() = 2

    companion object {
        const val CONST = 1
        fun foo(): Nested = null!!
    }
}
