// WITH_RUNTIME
import java.io.File
import java.io.BufferedReader

fun foo(reader: BufferedReader) {
    try <caret>{
        reader.readLine()
    }
    finally {
        reader.close()
    }
}