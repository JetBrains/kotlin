// WITH_STDLIB
enum class MyEnum {
    O;
    companion object {
        val K = "K"
    }
}

typealias MyAlias = MyEnum

fun box() = MyAlias.O.name + MyAlias.K