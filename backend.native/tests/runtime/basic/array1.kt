fun main(args : Array<String>) {
    val byteArray = ByteArray(5)
    byteArray[1] = 2
    byteArray[3] = 4
    println(byteArray[3].toString() + " "  + byteArray[1].toString())

    val shortArray = ShortArray(2)
    shortArray[0] = -1
    shortArray[1] = 1
    print(shortArray[1].toString())
    print(shortArray[0].toString())
    println()

    val intArray = IntArray(7)
    intArray[1] = 9
    intArray[3] = 6
    print(intArray[3].toString())
    print(intArray[1].toString())
    println()

    val longArray = LongArray(9)
    longArray[8] = 8
    longArray[3] = 3
    print(longArray[3].toString())
    print(longArray[8].toString())
    println()
}