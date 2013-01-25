// KT-2948 Assertion fails on a local enum

fun foo(): String {
   enum class E {
       OK
   }

   return E.OK.toString()
}

fun box(): String = foo()
