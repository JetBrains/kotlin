
fun main(args: Array<String>) {
    val a = ByteArray(1000000)

    var sum = 0
    for (b in a) {
        sum += b
    }

    println(sum)
}

