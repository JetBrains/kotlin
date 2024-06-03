fun main() {
    when (val var1 = 10; val var2 = 20; val var3 = 30; val var4 = 40; val var5 = 50; var1 + var2 + var3 + var4 + var5) {
10 -> println("Matched case: var1 = 10")
20 -> println("Matched case: var2 = 20")
30 -> println("Matched case: var3 = 30")
40 -> println("Matched case: var4 = 40")
50 -> println("Matched case: var5 = 50")
150 -> println("Matched case: sum = 150")
        else -> println("No match found")
    }
}