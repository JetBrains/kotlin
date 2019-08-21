 object Outer {
     val x = 0
     fun Nested.foo() {}
     class Nested {
         val y = x
         fun test() {
             foo()
         }
     }
 }
