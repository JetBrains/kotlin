// MAIN_FILE_NAME: I
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
class I(private val p: A) : A by p

interface A {
    var Int.zoo: Unit
    fun foo()
    fun Int.smth(): Short
    val foo: Int
    var bar: Long
    val Int.doo: String
}
