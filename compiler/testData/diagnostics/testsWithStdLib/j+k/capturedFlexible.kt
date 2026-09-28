// RUN_PIPELINE_TILL: CODEGEN
// FULL_JDK
fun foo(z: java.util.zip.ZipFile) {
    z.entries().asSequence()
}

/* GENERATED_FIR_TAGS: capturedType, flexibleType, functionDeclaration, javaFunction, outProjection */
