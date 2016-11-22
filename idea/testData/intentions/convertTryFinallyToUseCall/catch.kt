// IS_APPLICABLE: false
// WITH_RUNTIME
import java.io.File
import java.io.IOException

fun main(args: Array<String>) {
    val reader = File("hello-world.txt").bufferedReader()
    try {
        reader.readLine()
    }
    catch (e: IOException) {

    }
    <caret>finally {
        reader.close()
    }
}