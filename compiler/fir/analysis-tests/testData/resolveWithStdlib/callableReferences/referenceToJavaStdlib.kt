// ISSUE: KT-45052
// FULL_JDK
// JVM_TARGET: 1.8

import java.io.File
import java.nio.file.Files

fun detectDirsWithTestsMapFileOnly(file: File): List<String> {
    Files.walk(file.toPath()).filter(Files::isRegularFile)
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

interface A
fun foo(x: A, vararg strings: String) {}
fun takeFunWithA(func: (A) -> Unit) {}

fun test() {
    takeFunWithA(::foo)
}
