class Delegate {
}

fun Delegate.get(t: Any?, p: String): Int = 1

class A {
    val prop: Int by Delegate()
}

fun box(): String {
  return if(A().prop == 1) "OK" else "fail"
}
