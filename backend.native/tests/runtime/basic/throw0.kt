fun main(args : Array<String>) {
    val cond = 1
    if (cond == 2) throw RuntimeException()
    if (cond == 3) throw NoSuchElementException("no such element")
    if (cond == 4) throw Error("error happens")

    println("Done")
}
