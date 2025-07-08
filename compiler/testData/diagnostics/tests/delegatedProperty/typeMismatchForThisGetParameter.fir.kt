// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class B {
  val b: Int <!DELEGATION_OPERATOR_NONE_APPLICABLE!>by<!> Delegate()
}

val bTopLevel: Int <!DELEGATION_OPERATOR_NONE_APPLICABLE!>by<!> Delegate()

class A

class Delegate {
  fun getValue(t: A, p: KProperty<*>): Int {
    return 1
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, propertyDeclaration,
propertyDelegate, starProjection */
