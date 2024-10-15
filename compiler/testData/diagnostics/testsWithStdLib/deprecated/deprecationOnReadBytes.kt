// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
import java.io.InputStream

fun InputStream.test() {
    readBytes()

    <!DEPRECATION_ERROR!>readBytes<!>(1)
}
