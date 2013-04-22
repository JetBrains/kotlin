class Delegate {
}

class A {
    fun Delegate.get(t: Any?, p: String): Int = 1
    val prop: Int by Delegate()
}

fun box(): String {
  return if(A().prop == 1) "OK" else "fail"
}
