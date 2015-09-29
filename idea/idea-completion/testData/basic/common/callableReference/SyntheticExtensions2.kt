import java.io.File

class MyFile : File("") {
    val v = ::<caret>
}

// EXIST_JAVA_ONLY: { itemText: "freeSpace", tailText: " (from getFreeSpace())", attributes: "" }
// EXIST_JAVA_ONLY: { itemText: "isFile", tailText: " (from isFile())", attributes: "" }
// ABSENT: { itemText: "isFile", tailText: "()" }
