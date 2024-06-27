sealed class Bird

class Penguin : Bird()
class Ostrich : Bird()
class Kiwi : Bird()

sealed class Vehicle

class Car : Vehicle()
class Motocycle : Vehicle()

interface I

fun <T : Bird> simple(value: T) {
    val v = <!NO_ELSE_IN_WHEN!>when<!> (value) {
        is Penguin -> "Snow sledding on your belly sounds fun"
        is Ostrich -> "ostentatious and rich"
        is Kiwi -> "kiwiwiwiwi"
    }
}

fun <T> oneSealedOneUnrelated(value: T) where T : Bird, T : I {
    val v = <!NO_ELSE_IN_WHEN!>when<!> (value) {
        <!USELESS_IS_CHECK!>is Penguin<!> -> "Snow sledding on your belly sounds fun"
        <!USELESS_IS_CHECK!>is Ostrich<!> -> "ostentatious and rich"
        <!USELESS_IS_CHECK!>is Kiwi<!> -> "kiwiwiwiwi"
    }
}

fun <T> twoSealed(value: T) where T : Bird, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>Vehicle<!> {
    val v = <!NO_ELSE_IN_WHEN!>when<!> (value) {
        <!USELESS_IS_CHECK!>is Penguin<!> -> "Snow sledding on your belly sounds fun"
        <!USELESS_IS_CHECK!>is Ostrich<!> -> "ostentatious and rich"
        <!USELESS_IS_CHECK!>is Kiwi<!> -> "kiwiwiwiwi"
    }
}
