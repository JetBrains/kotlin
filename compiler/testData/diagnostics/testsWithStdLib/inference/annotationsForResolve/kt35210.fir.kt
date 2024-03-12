// !DIAGNOSTICS: -UNUSED_PARAMETER

@file:Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)

annotation class Anno

fun test(a: List<Class<Anno>>) {
    strictSelect(a, emptyList<Anno>().map { it.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>annotationClass<!>.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!> })
}

fun <@kotlin.internal.OnlyInputTypes S> strictSelect(arg1: S, arg2: S): S = TODO()
