// LANGUAGE: +StrictEquals
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Inherited

interface Base {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean
}

class Inherited : Base {
    override fun equals(other: Any?): Boolean = true
}
