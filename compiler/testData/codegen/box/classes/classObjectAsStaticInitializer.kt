// Enable for JS when it supports initializer of companion objects.
// TARGET_BACKEND: JVM
// see https://youtrack.jetbrains.com/issue/KT-11086
var global = 0;

class C {
  companion object {
      init {
        global = 1;
      }
  }
}

fun box(): String {
  if (global != 0) {
    return "fail1: global = $global"
  }

  val c = C()
  if (global == 1) return "OK" else return "fail2: global = $global"
}

