// WITH_RUNTIME
import java.io.File

fun main(args: Array<String>) {
    val reader = File("hello-world.txt").bufferedReader()
    try {
        // do stuff with reader
    }
    <caret>finally {
        reader.close()
    }
}