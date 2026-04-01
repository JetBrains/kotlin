// TARGET_BACKEND: JVM
// MODULE: lib
// WITH_STDLIB
// FILE: A.kt

package first.second

class FqName(val s: String)

@JvmField
val VOLATILE_ANNOTATION_FQ_NAME = FqName("volatile")

// MODULE: main(lib)
// WITH_STDLIB
// FILE: B.kt

import first.second.VOLATILE_ANNOTATION_FQ_NAME
import first.second.FqName

fun foo() = hasAnnotation(VOLATILE_ANNOTATION_FQ_NAME)

fun hasAnnotation(name: FqName): Boolean = true

fun box() = if (foo()) "OK" else "FAIL"
