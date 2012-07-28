package rendererTest

annotation class TheAnnotation {
}

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

//package rendererTest defined in root package
//internal final annotation class TheAnnotation defined in rendererTest
//public open class TheClass<out T : jet.Int, X> defined in rendererTest
//<out T : jet.Int> defined in rendererTest.TheClass
//<X> defined in rendererTest.TheClass
//private final val privateVal : jet.Int defined in rendererTest.TheClass
//internal final val shouldBeFinal : jet.Int defined in rendererTest.TheClass
//protected abstract fun foo() : Unit defined in rendererTest.TheClass
//private final class Inner defined in rendererTest.TheClass
//internal final class InternalClass defined in rendererTest
//internal trait TheTrait defined in rendererTest
//internal abstract fun abstractFun() : Unit defined in rendererTest.TheTrait
//object : rendererTest.TheClass<jet.Int, jet.Int> defined in rendererTest.TheTrait
//internal final fun classObjectFunction() : jet.Int defined in rendererTest.TheTrait.<no name provided>