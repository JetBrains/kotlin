// WITH_STDLIB
import java.io.IOException

typealias Throws = kotlin.jvm.Throws

class MyException : Exception()

class Foo {
    @Throws(IOException::class, MyException::class)
    fun ba<caret>r(): Boolean {
        return false
    }
}