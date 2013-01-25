trait Creator<T> {
    fun create() : T
}

class Actor(val code: String = "OK")

trait Factory : Creator<Actor>

class MyFactory() : Factory {
    override fun create(): Actor = Actor()
}

fun box() : String = MyFactory().create().code
