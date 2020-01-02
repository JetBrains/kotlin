// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val a: Int by A(1)

class A<T: Any>(i: T) {
  operator fun getValue(t: Any?, p: KProperty<*>): T {
    throw Exception()
  }
}
