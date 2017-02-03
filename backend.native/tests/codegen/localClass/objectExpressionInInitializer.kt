abstract class Father {
    abstract inner class InClass {
        abstract fun work(): String
    }
}

class Child : Father() {
    val ChildInClass : InClass

    init {
        ChildInClass =  object : Father.InClass() {
            override fun work(): String {
                return "OK"
            }
        }
    }
}

fun box(): String {
    return Child().ChildInClass.work()
}

fun main(args: Array<String>) {
    println(box())
}