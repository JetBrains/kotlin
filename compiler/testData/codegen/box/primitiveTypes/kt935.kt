// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

package bottles

fun box() : String {
    var bottles = 10
    while (bottles > 0) {
        print(bottlesOfBeer(bottles) + " on the wall, ")
        println(bottlesOfBeer(bottles) + ".")
        print("Take one down, pass it around, ")
        if (--bottles == 0) {
            println("no more bottles of beer on the wall.")
        }
        else {
            println(bottlesOfBeer(bottles) + " on the wall.")
        }
    }
    return "OK"
}

fun bottlesOfBeer(count : Int) :  String {
    val result = StringBuilder()
    result += count
    result += if (count > 1) " bottles of beer" else " bottle of beer"
    return result.toString() ?: ""
}

// An excerpt from the standard library
fun print(message : String) { System.out?.print(message) }
fun println(message : String) { System.out?.println(message) }
operator fun StringBuilder.plusAssign(o : Any) { append(o) }
val <T> Array<T>.isEmpty : Boolean get() = size == 0
