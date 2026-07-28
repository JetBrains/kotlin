// LANGUAGE: +StrictEquals
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Aliased

typealias BaseAlias = Base

open class Base

class Aliased : Base() {
    override fun equals(@EqualityBound(BaseAlias::class) other: Any?): Boolean = true
}
