// ISSUE: KT-58139

annotation class AnnKlass(val arg: String)

@AnnKlass("lhs" + "rhs")
fun foo() {}

const val BATCH_SIZE: Int = 16 * 1024
const val REPLACEMENT_BYTE: Byte = '?'.code.toByte()
