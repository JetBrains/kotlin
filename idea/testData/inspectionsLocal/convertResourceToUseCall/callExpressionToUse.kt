// WITH_RUNTIME
import java.io.File

fun main(args: Array<String>) {
    <caret>File("hello-world.txt").bufferedReader().close()
}
