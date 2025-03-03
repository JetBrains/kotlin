package test

interface MyBuilder {
    val name: String
}

class VarargArgumentWithFunctionalType {
    fun myDsl(vararg arguments: MyBuilder.(Int) -> Unit) {

    }
}
