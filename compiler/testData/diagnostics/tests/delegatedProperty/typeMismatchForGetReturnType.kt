// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val c: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): String {
    return ""
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, operator, propertyDeclaration,
propertyDelegate, starProjection, stringLiteral */
