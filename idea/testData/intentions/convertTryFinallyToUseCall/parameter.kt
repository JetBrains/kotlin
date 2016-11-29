// WITH_RUNTIME
import java.io.File
import java.io.BufferedReader

fun foo(reader: BufferedReader) {
    try {
        reader.readLine()
    }
    <caret>finally {
        reader.close()
    }
}