// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// API_VERSION: 1.2

import java.io.InputStream

fun InputStream.test() {
    readBytes()

    readBytes(1)
}
