// WITH_RUNTIME
import java.io.File
import java.io.BufferedReader

fun BufferedReader.foo() {
    try {
        this.readLine()
    }
    <caret>finally {
        this.close()
    }
}