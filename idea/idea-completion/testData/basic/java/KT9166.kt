import java.io.File

fun foo(file: File) {
    file.g<caret>
}

// EXIST: absolutePath
// ABSENT: getAbsolutePath