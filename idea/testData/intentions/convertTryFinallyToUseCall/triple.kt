// WITH_RUNTIME
import java.io.File

fun main(args: Array<String>) {
    val writer = File("hello-world.txt").bufferedWriter()
    try {
        writer.write("123")
        writer.newLine()
        writer.write("456")
    }
    <caret>finally {
        writer.close()
    }
}