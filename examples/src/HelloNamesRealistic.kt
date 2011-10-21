namespace HelloNamesRealistic

fun main(args : Array<String>) {
    val names = args.join(", ")
    println("Hello, $names!")
}

fun println(message : String) {
    // System.out is a nullable field
    System.out?.println(message)
}

fun <T> Iterable<T>.join(separator : String) : String? {
    val names = StringBuilder()
    forit (this) {(it: Iterator<T>): Unit =>
        names += it.next()
        if (it.hasNext())
            names += separator
    }
    return names.toString()
}

fun <T> forit(col : Iterable<T>, f : fun(Iterator<T>) : Unit) {
    val it = col.iterator()
    while (it.hasNext()) {
        f(it)
    }
}

fun StringBuilder.plusAssign(s : String) {
    this.append(s)
}
