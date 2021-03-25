// TARGET_BACKEND: JVM_IR
// FULL_JDK
// WITH_STDLIB

import java.nio.file.Files
import java.nio.file.Path

fun test(path: Path): Path? {
    return path.takeIf(Files::exists)
}

fun box() = "OK"
