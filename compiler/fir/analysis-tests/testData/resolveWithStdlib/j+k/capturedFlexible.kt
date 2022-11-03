// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FULL_JDK
fun foo(z: java.util.zip.ZipFile) {
    z.entries().asSequence()
}
