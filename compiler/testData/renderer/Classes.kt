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

private enum class TheEnum(val rgb : Int) {
    VAL1 : TheEnum(0xFF0000)
}

//package rendererTest defined in root package
//internal final annotation class TheAnnotation defined in rendererTest
//public open class TheClass<T : jet.Int, X> defined in rendererTest
//<T : jet.Int> defined in rendererTest.TheClass
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
//private final enum class TheEnum defined in rendererTest
//value-parameter val rgb : jet.Int defined in rendererTest.TheEnum.<init>
//internal final class VAL1 : rendererTest.TheEnum defined in rendererTest.TheEnum.class-object-for-TheEnum
//internal final val VAL1 : rendererTest.TheEnum.class-object-for-TheEnum.VAL1 defined in rendererTest.TheEnum.class-object-for-TheEnum
