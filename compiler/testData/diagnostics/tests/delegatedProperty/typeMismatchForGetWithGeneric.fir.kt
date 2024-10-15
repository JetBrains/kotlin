// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A

class B {
  val b: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate<A>()
}

val bTopLevel: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> Delegate<A>()

class C {
  val c: Int by Delegate<C>()
}

val cTopLevel: Int by Delegate<Nothing?>()

class Delegate<T> {
  operator fun getValue(t: T, p: KProperty<*>): Int {
    return 1
  }
}
