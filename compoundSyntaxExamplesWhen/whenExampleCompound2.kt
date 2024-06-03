fun main() {
    when (val var1 = 10; val var2 = 20; var1 + var2) {
10 -> println("Matched case: var1 = 10")
20 -> println("Matched case: var2 = 20")
30 -> println("Matched case: sum = 30")
        else -> println("No match found")
    }
}