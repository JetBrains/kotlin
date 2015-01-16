open class Klass {
    val used = ":)"
}

class Subklass: Klass()

fun main(args: Array<String>) {
    Subklass().used
}