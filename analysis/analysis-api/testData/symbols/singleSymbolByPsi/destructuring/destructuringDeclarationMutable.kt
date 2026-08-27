// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclaration

data class X(val a: Int, val b: Int)

fun main(x: X) {

    <expr>
    @AAA("y")
    var (@BBB("aaa") a, @BBB("bbb") b) = x
    </expr>
}

annotation class AAA(val value: String)
annotation class BBB(val value: String)
