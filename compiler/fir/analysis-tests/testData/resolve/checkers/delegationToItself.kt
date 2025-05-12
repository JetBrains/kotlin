// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidObjectDelegationToItself
// ISSUE: KT-17417

interface A {
    fun foo(): Int

    val bar: String
}

<!ABSTRACT_MEMBER_INCORRECTLY_DELEGATED_WARNING!>object B<!> : A by B

typealias D = C

<!ABSTRACT_MEMBER_INCORRECTLY_DELEGATED_WARNING!>object C<!> : A by D

object E : A by E {
    override fun foo() = 0

    override val bar = ""
}
