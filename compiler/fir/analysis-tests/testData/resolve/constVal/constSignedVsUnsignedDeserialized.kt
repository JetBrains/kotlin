// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FIR_DUMP
// MODULE: m1
// FILE: m1.kt

object O1 {
    const val ub1: UByte = 1u
    const val i2 = 2
    const val us3: UShort = 3u
    const val l4 = 4L
}

object O2 {
    const val b1: Byte = 1
    const val ui2 = 2u
    const val s3: Short = 3
    const val ul4 = 4uL
}

// MODULE: m2(m1)
// FILE: m2.kt

const val li42 = O1.l4 + O1.i2
const val s = O1.us3
const val b = O1.ub1

const val bs31 = O2.s3 + O2.b1
const val l = O2.ul4
const val i = O2.ui2

/* GENERATED_FIR_TAGS: const, functionDeclaration, integerLiteral, localProperty, objectDeclaration, propertyDeclaration,
unsignedLiteral */
