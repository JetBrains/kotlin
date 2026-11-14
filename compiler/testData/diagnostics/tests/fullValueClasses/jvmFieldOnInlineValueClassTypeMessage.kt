// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB
// ISSUE: KT-91600

@JvmInline
value class M(val v: Double)

value class P(val x: Int, val y: Int)

value class S(<!INAPPLICABLE_JVM_FIELD!>@JvmField<!> val m: M, val id: Int)

value class L(@JvmField val a: P, @JvmField val b: P)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, value */
