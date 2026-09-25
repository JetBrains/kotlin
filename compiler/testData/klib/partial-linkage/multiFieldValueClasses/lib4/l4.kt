value class Derived(val first: Int, val second: Int) : F(first, second)

fun mfvcToInline() = A(1).first
fun inlineToMfvc() = B(1).first

fun oneToTwoParameters() = C(1).first
fun twoToOneParameters() = D(1, 2).first

fun mfvcToAbstract() = E(1, 2).first
fun abstractToMfvc() = Derived(1, 2).first

fun classToMfvc() = G(1, 2).first
fun mfvcToClass() = H(1, 2).first

fun versionOverloadInlineToMfvc() = I(1).first
fun versionOverloadMfvc() = J(1).first
