// "Add 1st parameter to function 'printData'" "true"
// DISABLE-ERRORS
class Person
class Address

fun main() {
    val person = Person()
    val address = Address()
    printData(<caret>person, address)
}

fun printData(address: Address) {
}