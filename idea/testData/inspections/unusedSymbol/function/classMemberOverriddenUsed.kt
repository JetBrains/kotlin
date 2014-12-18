open class Klass {
    open fun used() {
    }
}

class Subklass: Klass() {
    override fun used() {
    }
}

fun main(args: Array<String>) {
    Subklass().used()
}