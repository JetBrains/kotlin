// "Create class 'BookKeeper'" "true"
// ERROR: Type mismatch: inferred type is BookKeeper but Unit was expected
// WITH_RUNTIME
package pack

import pack.Currrency.EUR

enum class Currrency { EUR }
class Item(val p1: Double, p2: Currrency)
class Transaction(vararg val p: Item)

fun place() {
    val transactions = listOf(Transaction(Item(10.0, EUR), Item(10.0, EUR)))
    if (transactions.isNotEmpty()) {
        return BookKee<caret>per(transactions)
    }
}