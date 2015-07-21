import java.io.File

fun foo(o: Any) {
    if (o is File) {
        o.<caret>
    }
}

// EXIST_JAVA_ONLY: { lookupString: "absolutePath", itemText: "absolutePath", tailText: " (from getAbsolutePath())", typeText: "String!" }
// ABSENT: getAbsolutePath
