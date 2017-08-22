import konan.*

fun main(args : Array<String>) {
    val data = immutableBinaryBlobOf(0x1, 0x2, 0x3, 0x7, 0x8, 0x9, 0x80, 0xff)
    for (b in data) {
        print("$b ")
    }
    println()

    val dataClone = data.toByteArray()
    dataClone.map { print("$it ") }
    println()
}