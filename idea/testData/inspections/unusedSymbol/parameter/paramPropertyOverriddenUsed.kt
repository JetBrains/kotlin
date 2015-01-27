open class Klass {
    open val used = ":)"
}

class Subklass: Klass(override val used: String)

fun main(args: Array<String>) {
    Subklass().used
}