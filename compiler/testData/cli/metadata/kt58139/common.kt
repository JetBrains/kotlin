// ISSUE: KT-58139

annotation class AnnKlass(val arg: String)

@AnnKlass("lhs" + "rhs")
fun foo() {}

const val BATCH_SIZE: Int = 16 * 1024
