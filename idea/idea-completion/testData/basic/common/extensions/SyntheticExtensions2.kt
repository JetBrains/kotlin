import java.io.File

fun File.foo() {
    <caret>
}

// EXIST_JAVA_ONLY: { lookupString: "absolutePath", itemText: "absolutePath", tailText: " for File", typeText: "String!" }
