// RUN_PIPELINE_TILL: BACKEND
// API_VERSION: 1.2

import java.io.InputStream

fun InputStream.test() {
    readBytes()

    readBytes(1)
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, integerLiteral */
