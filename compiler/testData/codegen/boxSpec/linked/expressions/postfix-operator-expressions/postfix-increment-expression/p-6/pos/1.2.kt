// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, postfix-operator-expressions, postfix-increment-expression -> paragraph 6 -> sentence 1
 * PRIMARY LINKS: expressions, prefix-expressions, prefix-increment-expression -> paragraph 5 -> sentence 1
 * overloadable-operators -> paragraph 4 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: check postfix increment expression has the same type as its operand expression
 */


fun box(): String {
    var a = A()
    val res: Any? = a++
    return if ((res !is B) && (res is A)) "OK"
    else "NOK"
}

open class A(var i :Int = 0) {

    open operator fun inc(): B {
        return B()
    }
}

class B() : A() {}
