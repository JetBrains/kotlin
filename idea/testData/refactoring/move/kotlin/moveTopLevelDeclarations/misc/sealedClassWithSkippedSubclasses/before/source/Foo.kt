// COMPILER_ARGUMENTS: -XXLanguage:-AllowSealedInheritorsInDifferentFilesOfSamePackage

package source

sealed class <caret>Expr
data class Const(val number: Double) : Expr()
data class Sum(val e1: Expr, val e2: Expr) : Expr()
object NotANumber : Expr()