package low

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno(val position: String)

const val constant = 0

class MyClass {
    @Anno("init annotation: $constant") i<caret>nit {
        @Anno("annotation inside")
        funInsideInit()
    }
}

fun anotherFun(): String = "str"
fun funInsideInit() = anotherFun()