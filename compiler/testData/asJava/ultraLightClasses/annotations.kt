import kotlin.reflect.KClass

/** should load cls */
annotation class Anno(val p: String = "", val x: Array<Anno> = arrayOf(Anno(p="a"), Anno(p="b")))

/** should load cls */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@Deprecated("This anno is deprecated, use === instead", ReplaceWith("this === other"))
annotation class Fancy

/** should load cls */
annotation class ReplaceWith(val expression: String)

/** should load cls */
annotation class Deprecated(
    val message: String,
    val replaceWith: ReplaceWith = ReplaceWith(""))

/** should load cls */
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

class Example(@field:Ann val foo,    // annotate Java field
              @get:Ann val bar,      // annotate Java getter
              @param:Ann val quux)   // annotate Java constructor parameter
