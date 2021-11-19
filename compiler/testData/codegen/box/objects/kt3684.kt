// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: NPE: null cannot be cast to not-null type IrPropertySymbol
// while translating this@R|/<anonymous>|.R|/X.n|
open class X(private val n: String) {

    fun foo(): String {
        return object : X("inner") {
            fun print(): String {
                return n;
            }
        }.print()
    }
}


fun box() : String {
  return X("OK").foo()
}

