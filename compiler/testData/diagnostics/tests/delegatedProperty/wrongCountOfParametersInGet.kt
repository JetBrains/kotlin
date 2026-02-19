// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: -ForbidGetSetValueWithTooManyParameters
// ISSUE: KT-77131

import kotlin.reflect.KProperty

class A {
  val a: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

val aTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>, a: Int): Int {
    return a
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, propertyDeclaration, propertyDelegate,
starProjection */
