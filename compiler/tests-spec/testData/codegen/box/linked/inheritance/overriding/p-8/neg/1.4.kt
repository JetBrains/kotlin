// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: inheritance, overriding -> paragraph 8 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: custom function: return type of a child override prop is not a subtype of return type of a base class
 * EXCEPTION: compiletime
 */

open class BaseCase1(){
    open fun foo(): Int = TODO()
}

open class ChildCase1 : BaseCase1(1, "") {
    override fun foo(): Any = 1 //(1)
}

fun box(): String {
    val childCase1 = ChildCase1()
    childCase1.foo() //resolves to (1)
    return "NOK"
}