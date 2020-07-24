// FULL_JDK
// FILE: YourException.java
class YourException extends Exception {

}

// FILE: test.kt
import java.io.PrintStream

class MyException : Exception()

fun test(e: MyException, stream: PrintStream) {
    e.printStackTrace() // Cannot be resolved with early J2K mapping due deriving of kotlin.Throwable instead of java.lang.Throwable
    e.printStackTrace(stream)
    val result = e.getLocalizedMessage()
}

fun test(e: YourException, stream: PrintStream) {
    e.printStackTrace()
    e.printStackTrace(stream)
    val result = e.getLocalizedMessage()
}

fun test(e: Exception, stream: PrintStream) {
    e.printStackTrace()
    e.printStackTrace(stream)
    val result = e.getLocalizedMessage()
}