class CallableObject {
    operator fun invoke() = Unit
}

class User(
    val name: String
) {
    val callableObject = CallableObject()
    val callableLambda: () -> Unit = {}

    fun checkName() = Unit

    operator fun invoke() = Unit
}

fun <caret>User.extFun() {
    name
    checkName()
    callableLambda()
    callableObject()
    this()
}