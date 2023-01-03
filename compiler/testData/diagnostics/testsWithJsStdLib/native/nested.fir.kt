object O

class TopLevel {
    external class A

    class B

    fun foo() = 23

    external fun bar(): Int

    val x = "a"

    external val y: String

    val O.u: String get() = "O.u"
}

external class TopLevelNative {
    external class A

    class B

    fun foo(): Int = definedExternally

    external fun bar(): Int

    val x: String = definedExternally

    external val y: String
}

fun topLevelFun() {
    external class A

    class B

    fun foo() = 23

    external fun bar(): Int
}
