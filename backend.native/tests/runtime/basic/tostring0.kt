fun main(args : Array<String>) {
    println(127.toByte().toString())
    println(255.toByte().toString())
    println(239.toShort().toString())
    println('A'.toString())
    println('Ё'.toString())
    println('ト'.toString())
    println(1122334455.toString())
    println(112233445566778899.toString())
    // Here we differ from Java, as have no dtoa() yet.
    println(3.14159265358.toString())
    // Here we differ from Java, as have no dtoa() yet.
    println(1e27.toFloat().toString())
    println(1e-300.toDouble().toString())
    println(true.toString())
    println(false.toString())
}