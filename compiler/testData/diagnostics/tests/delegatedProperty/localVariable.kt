// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

import kotlin.reflect.KProperty

class Local {
  fun foo() {
    val a: Int <!LOCAL_VARIABLE_WITH_DELEGATE!>by Delegate()<!>
  }
}

class Delegate {
  fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}