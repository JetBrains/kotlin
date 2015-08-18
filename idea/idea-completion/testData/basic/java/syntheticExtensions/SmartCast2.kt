import java.io.File

fun Any.foo() {
    if (this is File) {
        <caret>
    }
}

// EXIST: { lookupString: "absolutePath", itemText: "absolutePath", tailText: " (from getAbsolutePath())", typeText: "String!" }
// ABSENT: getAbsolutePath
