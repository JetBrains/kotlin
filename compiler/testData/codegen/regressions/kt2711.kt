class IntRange {
    fun contains(a: Int) = (1..2).contains(a)
}

class C() {
    fun rangeTo(i: Int) = IntRange()
}


fun main(args: Array<String>) {
    if (2 in C()..2) {
        System.out?.println(2)
    }
}