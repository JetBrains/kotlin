class Delegate {
   var inner = 1
   fun getValue(t: Any?, p: PropertyMetadata): Int = inner
   fun setValue(t: Any?, p: PropertyMetadata, i: Int) { inner = i }
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