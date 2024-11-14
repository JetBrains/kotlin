// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
fun foo(z: java.util.zip.ZipFile) {
    z.entries().asSequence()
}
