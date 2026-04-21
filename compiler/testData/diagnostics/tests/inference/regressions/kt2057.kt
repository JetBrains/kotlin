// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import java.util.ArrayList

fun <T> foo(a : T, b : Collection<T>, c : Int) {
}

fun <T> arrayListOf(vararg values: T): ArrayList<T> = throw Exception("$values")

val bar = <!NO_VALUE_FOR_PARAMETER!>foo<!>("", arrayListOf(), )
val bar2 = <!NO_VALUE_FOR_PARAMETER!>foo<!><String>("", arrayListOf(), )

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, outProjection, propertyDeclaration, stringLiteral,
typeParameter, vararg */
