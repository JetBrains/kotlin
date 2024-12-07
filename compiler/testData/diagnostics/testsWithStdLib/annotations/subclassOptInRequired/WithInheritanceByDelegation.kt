// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
interface ToBeInheritedByDelegation

open class InheritingByDelegationA(arg: ToBeInheritedByDelegation): <!OPT_IN_TO_INHERITANCE_ERROR!>ToBeInheritedByDelegation<!> by arg

@SubclassOptInRequired(ApiMarker::class)
open class InheritingByDelegationB(arg: ToBeInheritedByDelegation): ToBeInheritedByDelegation by arg
