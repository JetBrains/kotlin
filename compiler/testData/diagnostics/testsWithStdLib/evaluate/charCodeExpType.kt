// RUN_PIPELINE_TILL: FRONTEND
const val p1: Int = '\n'.code
const val p2: Long = '\n'.code.toLong()
const val p3: Byte = '\n'.code.toByte()
const val p4: Short = '\n'.code.toShort()

const val e2: Long = '\n'.<!INITIALIZER_TYPE_MISMATCH!>code<!>
const val e3: Byte = '\n'.<!INITIALIZER_TYPE_MISMATCH!>code<!>
const val e4: Short = '\n'.<!INITIALIZER_TYPE_MISMATCH!>code<!>

/* GENERATED_FIR_TAGS: const, propertyDeclaration */
