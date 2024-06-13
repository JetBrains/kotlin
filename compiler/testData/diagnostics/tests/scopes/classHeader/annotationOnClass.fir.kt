// DIAGNOSTICS: -UNUSED_PARAMETER

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
        <!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Nested<!>::class<!>,
        <!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Inner<!>::class<!>,
        <!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Interface<!>::class<!>,
        <!UNRESOLVED_REFERENCE!>CONST<!>,
        <!UNRESOLVED_REFERENCE!>Companion<!>.CONST,
        <!UNRESOLVED_REFERENCE!>Nested<!>.CONST,
        <!UNRESOLVED_REFERENCE!>Interface<!>.CONST,
        <!UNRESOLVED_REFERENCE!>a<!>,
        <!UNRESOLVED_REFERENCE!>b<!>()
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
