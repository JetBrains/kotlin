// WITH_STDLIB
// COMPILER_ARGUMENTS: -return-value-checker=full
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
interface X {
    fun x(): String
    @IgnorableReturnValue fun ignorable(): String

    class Impl: X {
        override fun x(): String = ""
        override fun ignorable(): String = ""
    }
}
