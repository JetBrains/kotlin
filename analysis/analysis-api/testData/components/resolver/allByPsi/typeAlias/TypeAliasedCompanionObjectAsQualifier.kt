package test

class MyClass {
    companion object {
        fun function() {}

        val property: Int = 10
    }
}

typealias MyAlias = MyClass

fun usage() {
    MyAlias.function()
    MyAlias.property

    MyAlias::function
    MyAlias::property
}