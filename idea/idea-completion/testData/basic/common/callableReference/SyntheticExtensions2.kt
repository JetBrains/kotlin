import java.io.File

fun f(file: File) {
    val v = file::<caret>
}

// EXIST_JAVA_ONLY: { itemText: "getFreeSpace", tailText: "()", attributes: "bold" }
// ABSENT: freeSpace
// EXIST_JAVA_ONLY: { itemText: "isFile", tailText: "()", attributes: "bold" }
// ABSENT: { itemText: "isFile", tailText: " (from isFile())" }
// ABSENT: separator
// ABSENT: listRoots
