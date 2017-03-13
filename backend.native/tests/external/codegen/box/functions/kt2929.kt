fun foo(): Int {
   val a = "test"
   val b = "test"
   return a.compareTo(b)
}

fun box(): String = if(foo() == 0) "OK" else "Fail"
