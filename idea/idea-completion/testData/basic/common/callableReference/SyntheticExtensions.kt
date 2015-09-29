import java.io.File

val v = File::<caret>

// EXIST_JAVA_ONLY: { itemText: "freeSpace", tailText: " (from getFreeSpace())", attributes: "bold" }
// EXIST_JAVA_ONLY: { itemText: "isFile", tailText: " (from isFile())", attributes: "bold" }
// ABSENT: { itemText: "isFile", tailText: "()" }
