import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        println(p.name)
        return 42
    }
}

val x: Int by Delegate()

fun main(args: Array<String>) {
    println(x)
}