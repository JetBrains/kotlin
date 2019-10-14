// FULL_JDK
class MyException : Exception()

fun test(e: MyException) {
    e.printStackTrace() // Cannot be resolved with early J2K mapping due deriving of kotlin.Throwable instead of java.lang.Throwable
}