// WITH_RUNTIME
import java.io.File

class MyFile : File("file")

fun foo(file: MyFile) {
    file.getAbsolutePath()<caret>
}