import java.io.File

class MyFile : File("") {
    val v = ::<caret>
}

// EXIST_JAVA_ONLY: { itemText: "getFreeSpace", tailText: "()", attributes: "" }
// ABSENT: freeSpace
// EXIST_JAVA_ONLY: { itemText: "isFile", tailText: "()", attributes: "" }
// ABSENT: { itemText: "isFile", tailText: " (from isFile())" }
