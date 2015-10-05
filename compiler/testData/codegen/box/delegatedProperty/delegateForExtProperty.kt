class Delegate {
  fun getValue(t: A, p: PropertyMetadata): Int = 1
}

val A.prop: Int by Delegate()

class A {
}

fun box(): String {
  return if(A().prop == 1) "OK" else "fail"
}
