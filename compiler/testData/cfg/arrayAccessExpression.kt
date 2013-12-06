
abstract class Ab {
    abstract fun getArray() : Array<Int>
}

fun test(ab: Ab) {
    ab.getArray()[1]
}