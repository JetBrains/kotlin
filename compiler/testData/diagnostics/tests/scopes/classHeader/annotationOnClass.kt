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
        <!ANNOTATION_PARAMETER_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Nested<!>::class<!>,
        <!ANNOTATION_PARAMETER_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Inner<!>::class<!>,
        <!ANNOTATION_PARAMETER_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Interface<!>::class<!>,
        <!UNRESOLVED_REFERENCE!>CONST<!>,
        <!ANNOTATION_PARAMETER_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Companion<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>CONST<!><!>,
        <!ANNOTATION_PARAMETER_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Nested<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>CONST<!><!>,
        <!ANNOTATION_PARAMETER_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>Interface<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>CONST<!><!>,
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
