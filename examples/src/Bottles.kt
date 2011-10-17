namespace bottles;

fun main(args: Array<String>) {
    var bottles: Int = 99;
    while(bottles > 0) {
        System.out?.print(bottlesOfBeer(bottles) + " on the wall, ")
        System.out?.println(bottlesOfBeer(bottles) + ".")
        System.out?.print("Take one down, pass it around, ")
        if (--bottles == 0) {
          System.out?.println("no more bottles of beer on the wall.")
        }
        else {
          System.out?.println(bottlesOfBeer(bottles) + " on the wall.")
        }
    }
}

fun bottlesOfBeer(count: Int): String {
    val result = StringBuilder()
    result.append(count)
    result.append(if (count > 1) " bottles of beer" else " bottle of beer")
    return result.toString() ?: ""
}
