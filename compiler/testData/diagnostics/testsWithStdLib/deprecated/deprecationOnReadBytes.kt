// RUN_PIPELINE_TILL: FRONTEND
import java.io.InputStream

fun InputStream.test() {
    readBytes()

    readBytes(<!TOO_MANY_ARGUMENTS!>1<!>)
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, integerLiteral */
