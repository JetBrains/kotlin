// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class D {
  var c: Int <!DELEGATION_OPERATOR_NONE_APPLICABLE!>by<!> Delegate()
}

var cTopLevel: Int <!DELEGATION_OPERATOR_NONE_APPLICABLE!>by<!> Delegate()

class A

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
  operator fun setValue(t: A, p: KProperty<*>, i: Int) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, operator,
propertyDeclaration, propertyDelegate, setter, starProjection */
