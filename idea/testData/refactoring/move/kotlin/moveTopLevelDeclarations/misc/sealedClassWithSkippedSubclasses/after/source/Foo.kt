// COMPILER_ARGUMENTS: -XXLanguage:-AllowSealedInheritorsInDifferentFilesOfSamePackage

package source

import target.Expr

data class Const(val number: Double) : Expr()
data class Sum(val e1: Expr, val e2: Expr) : Expr()
object NotANumber : Expr()