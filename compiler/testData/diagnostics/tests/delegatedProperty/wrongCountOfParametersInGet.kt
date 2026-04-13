// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidGetSetValueWithTooManyParameters
// ISSUE: KT-77131

import kotlin.reflect.KProperty

class A {
  val a: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate()
}

val aTopLevel: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate()

class Delegate {
  <!INAPPLICABLE_OPERATOR_MODIFIER_WARNING!>operator<!> fun getValue(t: Any?, p: KProperty<*>, a: Int): Int {
    return a
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, propertyDeclaration, propertyDelegate,
starProjection */
