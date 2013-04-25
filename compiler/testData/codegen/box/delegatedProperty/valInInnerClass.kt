class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int = 1
}

class A {
  inner class B {
      val prop: Int by Delegate()
  }
}

fun box(): String {
  return if(A().B().prop == 1) "OK" else "fail"
}
