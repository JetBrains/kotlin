// WITH_RUNTIME
import java.io.File
import java.io.BufferedReader

fun BufferedReader.foo() {
    try {
        readLine()
    }
    <caret>finally {
        close()
    }
}