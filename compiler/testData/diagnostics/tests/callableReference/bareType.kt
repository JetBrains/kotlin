fun f1() = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>::hashCode
fun f2() = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map.Entry<!>::hashCode

class Outer<T> {
    inner class Inner
}

fun f3() = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Outer.Inner<!>::hashCode
