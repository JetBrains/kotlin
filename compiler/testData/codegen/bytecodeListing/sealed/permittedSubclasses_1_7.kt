// IGNORE_DEXING
// JVM_TARGET: 17
// !LANGUAGE: +JvmPermittedSubclassesAttributeForSealed

// FILE: Expr.kt
sealed interface Expr

class VarExpr(val name: String) : Expr
class ParensExpr(val arg: Expr) : Expr

// FILE: Literals.kt
class IntExpr(val value: Int) : Expr
class DoubleExpr(val value: Double) : Expr

// FILE: UnaryOperators.kt
sealed class UnaryExpr(val arg: Expr) : Expr
class UnaryPlusExpr(arg: Expr) : UnaryExpr(arg)
class UnaryMinusExpr(arg: Expr) : UnaryExpr(arg)

// FILE: BinaryOperators.kt
sealed class BinaryExpr(val arg1: Expr, val arg2: Expr) : Expr
class BinaryPlusExpr(arg1: Expr, arg2: Expr) : BinaryExpr(arg1, arg2)
class BinaryMinusExpr(arg1: Expr, arg2: Expr) : BinaryExpr(arg1, arg2)
class BinaryMulExpr(arg1: Expr, arg2: Expr) : BinaryExpr(arg1, arg2)
class BinaryDivExpr(arg1: Expr, arg2: Expr) : BinaryExpr(arg1, arg2)
