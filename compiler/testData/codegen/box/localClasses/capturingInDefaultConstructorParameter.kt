// IGNORE_BACKEND: JVM
open class Base(val fn: () -> String)

class Test(x: String) :
    Base({
             class Local(val t: String = x)
             Local().t
         })

fun box() =
    Test("OK").fn()