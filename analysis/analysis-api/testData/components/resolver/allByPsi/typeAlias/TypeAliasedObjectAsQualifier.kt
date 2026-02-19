package test

object MyObject {
    fun function() {}

    val property: Int = 10
}

typealias MyAlias = MyObject

fun usage() {
    MyAlias.function()
    MyAlias.property

    MyAlias::function
    MyAlias::property
}