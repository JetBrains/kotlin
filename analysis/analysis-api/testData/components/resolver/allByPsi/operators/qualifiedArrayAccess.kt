class MyClass {
   operator fun get(argument: String) {}
   val itself: MyClass get() = this
   val nullableItself: MyClass? get() = this

   fun itselfFun(): MyClass = this
   fun nullableItselfFun(): MyClass? = this

   val itselfCallable: (String) -> MyClass = { _ -> MyClass() }
}

fun MyClass.itselfExt(): MyClass = this

fun main() {
   val s: MyClass? = MyClass()
   s?.itself["1"]
   s!!.itself["2"]
   s["3"]

   s.nullableItself?.get("4")
   s.nullableItself!!["5"]

   s?.itselfFun()["6"]
   s.nullableItselfFun()?.get("7")
   s.nullableItselfFun()!!["8"]

   s?.itselfCallable("10")["11"]

   s?.nullableItself?.itself["12"]

   s?.itselfExt()["13"]
}

class SmartNode {
   operator fun get(argument: String) {}
   val child: SmartNode? = null
}

fun smartCastSafeCallResult(node: SmartNode?) {
   if (node?.child != null) {
      node?.child["9"]
   }
}
