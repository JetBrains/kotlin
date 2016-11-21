fun main(args : Array<String>) {
    val byteArray = ByteArray(3)
    byteArray[0] = 2
    byteArray[1] = 3
    byteArray[2] = 4
    for (item in byteArray) {
        println(item)
    }
}