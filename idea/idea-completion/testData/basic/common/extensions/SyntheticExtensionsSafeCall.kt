import java.io.File

fun foo(file: File?) {
    file?.<caret>
}

// EXIST_JAVA_ONLY: { lookupString: "absolutePath", itemText: "absolutePath", tailText: " (from getAbsolutePath())", typeText: "String!" }
// ABSENT: getAbsolutePath
