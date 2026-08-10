// LANGUAGE: +StrictEquals
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass

class Explicit {
    override fun equals(@EqualityBound(Explicit::class) other: Any?): Boolean = true
}
