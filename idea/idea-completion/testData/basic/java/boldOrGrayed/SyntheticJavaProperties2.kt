import java.io.File

fun foo(file: File) {
    file.<caret>
}

// EXIST: { lookupString: "absolutePath", itemText: "absolutePath", tailText: " (from getAbsolutePath())", typeText: "String!", attributes: "bold" }
// EXIST: { lookupString: "isFile", itemText: "isFile", tailText: " (from isFile())", typeText: "Boolean", attributes: "bold" }
