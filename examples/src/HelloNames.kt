namespace HelloNames

fun main(args : Array<String>) {
    var names : String = ""

    for (i in args.indices) {
        names += args[i]
        if (i + 1 < args.size)
            names += ", "
    }
    // string templates
    println("Hello, $names!")
}

fun println(message : String) {
    // System.out is a nullable field
    System.out?.println(message)
}
