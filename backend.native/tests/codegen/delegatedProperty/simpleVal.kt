import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        println(p.name)
        return 42
    }
}

class C {
    val x: Int by Delegate()
}

fun main(args: Array<String>) {
    println(C().x)
}