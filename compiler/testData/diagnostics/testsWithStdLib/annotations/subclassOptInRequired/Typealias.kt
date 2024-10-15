// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
interface Interface

typealias TypeAlias = Interface

class InterfaceInheritorB: <!OPT_IN_TO_INHERITANCE_ERROR!>TypeAlias<!>
