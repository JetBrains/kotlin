fun usage() {
    1 > 2
    1.compareTo(2) > 0

    "3" < "4"
    val number = 5
    val myClass = MyClass()
    myClass >= number
    myClass.compareTo(number)
}

class MyClass {
    operator fun compareTo(i: Int): Int = i
}
