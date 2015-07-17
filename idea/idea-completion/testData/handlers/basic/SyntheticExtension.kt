import java.io.File

fun foo(file: File) {
    file.abs<caret>
}

// ELEMENT: absolutePath
