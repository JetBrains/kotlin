fun sum(a:Int): Int = a + 33

fun main(args:Array<String>) {
    if (sum(2) != 35) throw Error()
}