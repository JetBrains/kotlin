class A(val a: Int = 0)

fun box(): String {
   if (A().a == 0 && A(1).a == 1) {
      return "OK"
   }
   return "fail"
}
