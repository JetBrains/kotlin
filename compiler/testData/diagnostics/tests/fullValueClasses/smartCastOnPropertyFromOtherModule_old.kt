// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88589
// LANGUAGE: +FullValueClasses, -AllowSmartCastsOnValueClassUnderlyingProperties
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt
package lib

value class MultiField(val first: Int?, val second: String) {
    val computed: Int? get() = first
}

@JvmInline
value class SingleField(val only: Int?)

class Identity(val first: Int?)

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.Identity
import lib.MultiField
import lib.SingleField

fun multiField(v: MultiField): Int = if (v.first != null) <!SMARTCAST_IMPOSSIBLE!>v.first<!>.inc() else 0

fun singleField(v: SingleField): Int = if (v.only != null) <!SMARTCAST_IMPOSSIBLE!>v.only<!>.inc() else 0

fun bodyPropertyWithGetter(v: MultiField): Int = if (v.computed != null) <!SMARTCAST_IMPOSSIBLE!>v.computed<!>.inc() else 0

fun identity(v: Identity): Int = if (v.first != null) <!SMARTCAST_IMPOSSIBLE!>v.first<!>.inc() else 0

value class SameModuleMultiField(val first: Int?, val second: String)

fun sameModule(v: SameModuleMultiField): Int = if (v.first != null) v.first.inc() else 0

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, getter, ifExpression, integerLiteral,
nullableType, primaryConstructor, propertyDeclaration, smartcast, value */
