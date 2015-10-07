import java.io.File

fun main(args: Array<String>) {
    val f: File = MyFile()
    <caret>f.absoluteFile
}

class MyFile: File("")

// EXIST: absoluteFile
// NOTHING_ELSE

// RUNTIME_TYPE: MyFile