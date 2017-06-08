fun main(args : Array<String>) {
    val hello = "Hello"
    val array = hello.toCharArray()
    for (ch in array) {
        print(ch)
        print(" ")
    }
    println()
    println(fromCharArray(array, 0, array.size))
}