
fun interface MyInterface {
    fun execute()
}

typealias MyTypeAlias = MyInterface

fun usage() {
    MyTyp<caret>eAlias {

    }
}
