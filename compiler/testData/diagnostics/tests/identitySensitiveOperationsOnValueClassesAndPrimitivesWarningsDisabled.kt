// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// FIR_IDENTICAL
// LANGUAGE: +DisableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives
@JvmInline
value class VcString(val s: String)

fun test(p1: Int, p2: VcString) {
    System.identityHashCode(p1)
    System.identityHashCode(p2)
    System.identityHashCode(Integer.valueOf(1))
}
