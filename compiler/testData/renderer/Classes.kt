package rendererTest

annotation class TheAnnotation

[TheAnnotation]
public open class TheClass<out T : Int, X> {
  private val privateVal : Int = 5

  val shouldBeFinal : Int = 5

  protected abstract fun foo() {}

  private class Inner {}
}

internal class InternalClass {
}

trait TheTrait {
  fun abstractFun()

  class object : TheClass<Int, Int> {
    fun classObjectFunction() : Int {}
  }
}

public class WithReified<reified A, reified out B>

//package rendererTest defined in root package
//internal final annotation class TheAnnotation : jet.Annotation defined in rendererTest
//public ctor TheAnnotation() defined in rendererTest.TheAnnotation
//rendererTest.TheAnnotation public open class TheClass<out T : jet.Int, X> defined in rendererTest
//public ctor TheClass<out T : jet.Int, X>() defined in rendererTest.TheClass
//<out T : jet.Int> defined in rendererTest.TheClass
//<X> defined in rendererTest.TheClass
//private final val privateVal : jet.Int defined in rendererTest.TheClass
//internal final val shouldBeFinal : jet.Int defined in rendererTest.TheClass
//protected abstract fun foo() : Unit defined in rendererTest.TheClass
//private final class Inner defined in rendererTest.TheClass
//public ctor Inner() defined in rendererTest.TheClass.Inner
//internal final class InternalClass defined in rendererTest
//public ctor InternalClass() defined in rendererTest.InternalClass
//internal trait TheTrait defined in rendererTest
//internal abstract fun abstractFun() : Unit defined in rendererTest.TheTrait
//class object : rendererTest.TheClass<jet.Int, jet.Int> defined in rendererTest.TheTrait
//private ctor <class-object-for-TheTrait>() defined in rendererTest.TheTrait.<class-object-for-TheTrait>
//internal final fun classObjectFunction() : jet.Int defined in rendererTest.TheTrait.<class-object-for-TheTrait>
//public final class WithReified<reified A, reified out B> defined in rendererTest
//public ctor WithReified<reified A, reified out B>() defined in rendererTest.WithReified
//<reified A> defined in rendererTest.WithReified
//<reified out B> defined in rendererTest.WithReified