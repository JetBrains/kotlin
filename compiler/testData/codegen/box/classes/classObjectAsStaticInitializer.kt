var global = 0;

class C {
  default object {
      {
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

