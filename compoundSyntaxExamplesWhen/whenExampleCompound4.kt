fun main() {
    when (val var1 = 10; val var2 = 20; val var3 = 30; val var4 = 40; var1 + var2 + var3 + var4) {
10 -> println("Matched case: var1 = 10")
20 -> println("Matched case: var2 = 20")
30 -> println("Matched case: var3 = 30")
40 -> println("Matched case: var4 = 40")
100 -> println("Matched case: sum = 100")
        else -> println("No match found")
    }
}