// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt

import kotlin.test.assertEquals

// Private classes
private open class A {
    public fun foo1() = "PASS"
    internal fun foo2() = "PASS"
    protected fun foo3() = "PASS"
}

private class B:A() {
    fun foo4() = foo3()
}

// Private interfaces
private interface C {
    fun foo() = "PASS"
}

private class D: C 

fun runner(): String {
   assertEquals(B().foo1(), "PASS")
   assertEquals(B().foo2(), "PASS")
   assertEquals(B().foo4(), "PASS")

   assertEquals(D().foo(), "PASS")

   // Objects
   object : A(){
       fun foo4() = foo3()
   }.apply {
       assertEquals(foo1(), "PASS")
       assertEquals(foo2(), "PASS")
       assertEquals(foo4(), "PASS")
   }

   // Function local classes
   abstract class E {
       public open fun foo1() = "PASS"
       internal open fun foo2() = "PASS"
       protected open fun foo3() = "PASS"
   }
   class F : E() {
       fun foo4() = foo3()
   }
   assertEquals(F().foo1(), "PASS")
   assertEquals(F().foo2(), "PASS")
   assertEquals(F().foo4(), "PASS")
   return("OK")
}

// MODULE: main(lib)
// FILE: main.kt
fun box() = runner()

