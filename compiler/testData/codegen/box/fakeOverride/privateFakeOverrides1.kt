// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt

import kotlin.test.assertEquals

// Private classes
private open class A {
    public open fun foo1() = "FAIL"
    internal open fun foo2() = "FAIL"
    protected open fun foo3() = "FAIL"
    private fun foo4() = "FAIL"
}

private class B:A() {
    override public fun foo1() = "PASS"
    override internal fun foo2() = "PASS"
    override protected fun foo3() = "PASS"
    private fun foo4() = "PASS"
    fun foo5() = foo3()
    fun foo6() = foo4()
}

private abstract class G {
    public abstract fun foo1()
    internal abstract fun foo2()
    protected abstract fun foo3()
    private fun foo4() = "FAIL"
}

private class H:A() {
    override public fun foo1() = "PASS"
    override internal fun foo2() = "PASS"
    override protected fun foo3() = "PASS"
    private fun foo4() = "PASS"
    fun foo5() = foo3()
    fun foo6() = foo4()
}


// Private interfaces
private interface C {
    fun foo() = "FAIL"
}

private class D: C {
    override fun foo() = "PASS"
}

fun runner(): String {
   assertEquals(B().foo1(), "PASS")
   assertEquals(B().foo2(), "PASS")
   assertEquals(B().foo5(), "PASS")
   assertEquals(B().foo6(), "PASS")

   assertEquals(H().foo1(), "PASS")
   assertEquals(H().foo2(), "PASS")
   assertEquals(H().foo5(), "PASS")
   assertEquals(H().foo6(), "PASS")

   assertEquals(D().foo(), "PASS")

   // Objects
   object : A(){
        override public fun foo1() = "PASS"
        override internal fun foo2() = "PASS"
        override protected fun foo3() = "PASS"
        private fun foo4() = "PASS"
        fun foo5() = foo3()
        fun foo6() = foo4()
   }.apply {
       assertEquals(foo1(), "PASS")
       assertEquals(foo2(), "PASS")
       assertEquals(foo5(), "PASS")
       assertEquals(foo6(), "PASS")
   }

   // Function local classes
   open class E {
       public open fun foo1() = "FAIL"
       internal open fun foo2() = "FAIL"
       protected open fun foo3() = "FAIL"
       private fun foo4() = "FAIL"
   }
   class F : E() {
       public override fun foo1() = "PASS"
       internal override fun foo2() = "PASS"
       protected override fun foo3() = "PASS"
       private fun foo4() = "PASS"
       fun foo5() = foo3()
       fun foo6() = foo4()
   }
   assertEquals(F().foo1(), "PASS")
   assertEquals(F().foo2(), "PASS")
   assertEquals(F().foo5(), "PASS")
   assertEquals(F().foo6(), "PASS")

   return "OK"
}

// MODULE: main(lib)
// FILE: main.kt
fun box() = runner()
