// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

interface T {
  val a: Int <!DELEGATED_PROPERTY_IN_INTERFACE!>by Delegate()<!>
}

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, nullableType,
operator, propertyDeclaration, propertyDelegate, starProjection */
