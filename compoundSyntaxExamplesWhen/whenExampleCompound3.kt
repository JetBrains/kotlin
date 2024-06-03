fun main() {
    when (val var1 = 10; val var2 = 20; val var3 = 30; var1 + var2 + var3) {
10 -> println("Matched case: var1 = 10")
20 -> println("Matched case: var2 = 20")
30 -> println("Matched case: var3 = 30")
60 -> println("Matched case: sum = 60")
        else -> println("No match found")
    }
}