fun <T> Iterable<T>.first() : T? {
    return this.iterator()?.next()
}

fun main(args : Array<String>) {
    val test = java.util.HashSet<Int>()
    test.<caret>
}

// EXIST: first
