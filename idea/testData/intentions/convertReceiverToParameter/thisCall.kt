// WITH_RUNTIME
class CallableObject {
    operator fun User.invoke() = println("User.invoke")
}

class User {
    operator fun CallableObject.invoke() = println("CallableObject.invoke")
}

fun <caret>User.extFun() {
    val callableObject = CallableObject()
    with(callableObject) {
        this()
        this@extFun()
    }
}