// IGNORE_BACKEND_FIR: JVM_IR
// FILE: 1.kt

import testing.ClassWithInternals

public class HelloServer() : ClassWithInternals() {
    public override fun start() {
        val test = foo() + someGetter //+ some
    }
}

fun box() : String {
    HelloServer().start()
    return "OK"
}

// FILE: 2.kt

package testing; // There is no error if both files are in default package

public abstract class ClassWithInternals {
   protected var some: Int = 0;
   protected var someGetter: Int = 0
      get() = 5

   protected fun foo() : Int = 0

   public abstract fun start() : Unit;
}
