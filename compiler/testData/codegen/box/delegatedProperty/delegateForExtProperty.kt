class Delegate {
  fun get(t: A, p: String): Int = 1
}

val A.prop: Int by Delegate()

class A {
}

fun box(): String {
  return if(A().prop == 1) "OK" else "fail"
}
