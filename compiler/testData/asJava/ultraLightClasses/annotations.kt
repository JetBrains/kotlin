import kotlin.reflect.KClass

@Target(*[AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION])
annotation class Anno2()

@Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION])
annotation class Anno3()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
annotation class Anno4()

@Target(*arrayOf(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION))
annotation class Anno5()

@Target(allowedTargets = arrayOf(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION))
annotation class Anno6()

annotation class AnnoWithCompanion() {
    companion object {
        fun foo() {}
        @JvmField
        val x: Int = 42
    }
}

annotation class Anno(val p: String = "", val x: Array<Anno> = arrayOf(Anno(p = "a"), Anno(p = "b")))
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@Deprecated("This anno is deprecated, use === instead", ReplaceWith("this === other"))
annotation class Fancy

annotation class ReplaceWith(val expression: String)

annotation class AnnotatedAttribute(@get:Anno val x: String)

annotation class Deprecated(
    val message: String,
    val replaceWith: ReplaceWith = ReplaceWith(""))

annotation class Ann(val arg1: KClass<*>, val arg2: KClass<out Any>)


@Anno class F: Runnable {
  @Anno("f") fun f(@Anno p: String) {}
  @Anno("p") var prop = "x"
}


class Foo @Anno constructor(dependency: MyDependency) {
  var x: String? = null
        @Anno set

    @Anno
    fun String.f4() {}
}

@Ann(String::class, Int::class) class MyClass

class Example(@field:Ann val foo: String,    // annotate Java field
              @get:Ann val bar: String,      // annotate Java getter
              @param:Ann val quux: String)   // annotate Java constructor parameter
