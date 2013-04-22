class Delegate<T>(var inner: T) {
    fun get(t: Any?, p: String): T = inner
    fun set(t: Any?, p: String, i: T) { inner = i }
}

class A {
  inner class B {
      var prop: Int by Delegate(1)
  }
}

fun box(): String {
    val c = A().B()
    if(c.prop != 1) return "fail get"
    c.prop = 2
    if (c.prop != 2) return "fail set"
    return "OK"
}
