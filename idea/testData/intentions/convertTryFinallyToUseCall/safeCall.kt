// WITH_RUNTIME
import java.io.File
import java.io.BufferedReader

fun bar() {}

fun foo(reader: BufferedReader?) {
    try {
        reader?.readLine()
        bar()
    }
    <caret>finally {
        reader?.close()
    }
}