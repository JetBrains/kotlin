// IGNORE_BACKEND_FIR: JVM_IR
interface Creator<T> {
    fun create() : T
}

class Actor(val code: String = "OK")

interface Factory : Creator<Actor>

class MyFactory() : Factory {
    override fun create(): Actor = Actor()
}

fun box() : String = MyFactory().create().code
