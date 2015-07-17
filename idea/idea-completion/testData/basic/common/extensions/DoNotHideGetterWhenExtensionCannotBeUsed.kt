import java.io.File

fun File.foo(absolutePath: String?) {
    <caret>
}

// EXIST_JAVA_ONLY: getAbsolutePath
// ABSENT: { itemText: "absolutePath", typeText: "String" }
