fun main(args: Array<String>) {
    val a = Array<Byte>(100000, { i -> i.toByte()})

    var sum = 0
    for (b in a) {
        sum += b
    }

    println(sum)
}

