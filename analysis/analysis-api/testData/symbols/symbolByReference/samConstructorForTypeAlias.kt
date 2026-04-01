// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1

fun interface MyInterface {
    fun execute()
}

typealias MyTypeAlias = MyInterface

fun usage() {
    MyTyp<caret>eAlias {

    }
}
