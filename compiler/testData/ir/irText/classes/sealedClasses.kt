// FIR_IDENTICAL
// SKIP_KLIB_TEST
// REASON: Sealed subclasses is not deserialized
sealed class Expr {
    class Const(val number: Double) : Expr()
    class Sum(val e1: Expr, val e2: Expr) : Expr()
    object NotANumber : Expr()
}
