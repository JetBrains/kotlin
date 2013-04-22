class Delegate {
   var inner = 1
   fun get(t: Any?, p: String): Int = inner
   fun set(t: Any?, p: String, i: Int) { inner = i }
}

class A {
   private var prop: Int by Delegate()

   fun test(): String {
     if(prop != 1) return "fail get"
     prop = 2
     if (prop != 2) return "fail set"
     return "OK"
   }
}

fun box(): String {
  return A().test()
}