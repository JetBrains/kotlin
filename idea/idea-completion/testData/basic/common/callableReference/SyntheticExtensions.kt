// FIR_COMPARISON
import java.io.File

val v = File::<caret>

// EXIST_JAVA_ONLY: { itemText: "getFreeSpace", tailText: "()", attributes: "bold" }
// ABSENT: freeSpace
// EXIST_JAVA_ONLY: { itemText: "isFile", tailText: "()", attributes: "bold" }
// ABSENT: { itemText: "isFile", tailText: " (from isFile())" }
