// DO_NOT_CHECK_SYMBOL_RESTORE_K1

fun interface MyInterface {
    fun execute()
}

typealias MyTypeAlias = MyInterface

fun usage() {
    MyTyp<caret>eAlias {

    }
}
