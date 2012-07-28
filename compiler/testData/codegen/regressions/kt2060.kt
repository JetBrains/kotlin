package testing; // There is no error if both files are in default package

public abstract class ClassWithInternals {
   protected var some: Int = 0;
   protected var someGetter: Int = 0
      get() = 5

   protected fun foo() : Int = 0

   public abstract fun start() : Unit;
}
