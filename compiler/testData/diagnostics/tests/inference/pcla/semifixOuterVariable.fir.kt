// ISSUE: KT-69170

interface OuterController<T1 : Any>
interface NestedController<E2 : Any> {
    fun nestedCallInsideNestedPCLA(resolver: (E2) -> Any?)

    fun add(e2: E2) {}
}

fun <T3 : Any> outerPCLACall(
    l1: OuterController<T3>.() -> Unit,
) {}

fun <T4 : Any, E4 : T4> OuterController<T4>.nestedPCLA(
    sourceOfLowerConstraint: E4,
    l2: NestedController<E4>.() -> Unit,
) {}

open class Base
class ClassWithParamInMemberScope(val param: Int) : Base()

val sourceOfConstraintForNestedPCLA: ClassWithParamInMemberScope = TODO()

fun main() {
    outerPCLACall {
        // T3v = T4v
        // E4v <: T4v
        // E4v <: T3v
        nestedPCLA(
            // ClassWithParamInMemberScope <: E4v
            // ClassWithParamInMemberScope <: T4v
            // ClassWithParamInMemberScope <: T3v
            sourceOfConstraintForNestedPCLA
        ) {
            nestedCallInsideNestedPCLA { x -> // x has a type of E4v
                // Trying to look into a member scope of E4v.
                // E4v has only ClassWithParamInMemberScope lower constraint.
                // But potentially there might be a different one added later, thus we're not ready to fix it
                x.<!UNRESOLVED_REFERENCE!>param<!> // OK in K1, Error in K2, should it be OK?
            }

            // Base <: E4v
            add(Base())
        }
    }
}
