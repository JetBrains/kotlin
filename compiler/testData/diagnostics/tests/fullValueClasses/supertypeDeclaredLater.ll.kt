// LL_FIR_DIVERGENCE
// The compiler doesn't compute the representation of value classes whose status is resolved as a supertype first.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// FILE: a.kt

value class Sub(val a: Int, val b: Long) : SBase()

value class OtherSub(val a: Int) : OtherBase(a)

// FILE: b.kt

sealed value class SBase

abstract value class OtherBase(x: Int)

value class SameFileSub(override val a: String) : SameFileBase()

abstract value class SameFileBase {
    abstract val a: String
}

/* GENERATED_FIR_TAGS: classDeclaration, override, primaryConstructor, propertyDeclaration, sealed, value */
