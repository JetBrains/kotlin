
class Box<T>(t: T) {
    var value = t
}

fun main(args: Array<String>) {
    val box: Box<Int> = Box<Int>(17)
    println(box.value)
}

