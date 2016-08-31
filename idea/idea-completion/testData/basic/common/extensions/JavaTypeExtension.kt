import java.util.* // temporary until typealiases for collection are introduced in JVM

fun <T> Iterable<T>.first() : T? {
    return this.iterator()?.next()
}

fun main(args : Array<String>) {
    val test = HashSet<Int>()
    test.<caret>
}

// EXIST: first
