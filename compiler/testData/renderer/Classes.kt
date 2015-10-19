package rendererTest

annotation class TheAnnotation
annotation class AnotherAnnotation

@TheAnnotation
public open class TheClass<out T : Int, X> @AnotherAnnotation constructor() {
  private val privateVal: Int = 5

  val shouldBeFinal: Int = 5

  val <T> T.checkTypeParameterScope: Int get() = 6
  val <T> checkTypeParameterScope2 = 6

  protected abstract fun foo() {}

  @TheAnnotation private constructor()

  private class Inner {}
}

internal class InternalClass {
}

interface TheTrait {
  fun abstractFun()

  companion object : TheClass<Int, Int> {
    fun classObjectFunction(): Int {}
  }
}

public class WithReified<reified A, reified out B>

public interface TwoUpperBounds<T> where T : Number, T : Any

//package rendererTest
//public final annotation class TheAnnotation : kotlin.Annotation defined in rendererTest
//public constructor TheAnnotation() defined in rendererTest.TheAnnotation
//public final annotation class AnotherAnnotation : kotlin.Annotation defined in rendererTest
//public constructor AnotherAnnotation() defined in rendererTest.AnotherAnnotation
//@rendererTest.TheAnnotation public open class TheClass<out T : kotlin.Int, X> defined in rendererTest
//<out T : kotlin.Int> defined in rendererTest.TheClass
//<X> defined in rendererTest.TheClass
//@rendererTest.AnotherAnnotation public constructor TheClass<out T : kotlin.Int, X>() defined in rendererTest.TheClass
//private final val privateVal: kotlin.Int defined in rendererTest.TheClass
//public final val shouldBeFinal: kotlin.Int defined in rendererTest.TheClass
//public final val <T> T.checkTypeParameterScope: kotlin.Int defined in rendererTest.TheClass
//<T> defined in rendererTest.TheClass.checkTypeParameterScope
//public final fun T.<get-checkTypeParameterScope>(): kotlin.Int defined in rendererTest.TheClass
//public final val <T> checkTypeParameterScope2: kotlin.Int defined in rendererTest.TheClass
//<T> defined in rendererTest.TheClass.checkTypeParameterScope2
//protected abstract fun foo(): kotlin.Unit defined in rendererTest.TheClass
//@rendererTest.TheAnnotation private constructor TheClass<out T : kotlin.Int, X>() defined in rendererTest.TheClass
//private final class Inner defined in rendererTest.TheClass
//public constructor Inner() defined in rendererTest.TheClass.Inner
//internal final class InternalClass defined in rendererTest
//public constructor InternalClass() defined in rendererTest.InternalClass
//public interface TheTrait defined in rendererTest
//public abstract fun abstractFun(): kotlin.Unit defined in rendererTest.TheTrait
//public companion object : rendererTest.TheClass<kotlin.Int, kotlin.Int> defined in rendererTest.TheTrait
//private constructor Companion() defined in rendererTest.TheTrait.Companion
//public final fun classObjectFunction(): kotlin.Int defined in rendererTest.TheTrait.Companion
//public final class WithReified<reified A, reified out B> defined in rendererTest
//public constructor WithReified<reified A, reified out B>() defined in rendererTest.WithReified
//<reified A> defined in rendererTest.WithReified
//<reified out B> defined in rendererTest.WithReified
//public interface TwoUpperBounds<T : kotlin.Number> where T : kotlin.Any defined in rendererTest
//<T : kotlin.Number & kotlin.Any> defined in rendererTest.TwoUpperBounds