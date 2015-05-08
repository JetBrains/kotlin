// "Change 'A.hasNext' function return type to 'Boolean'" "true"
abstract class A {
    abstract fun hasNext
    abstract fun next(): Int
    abstract fun iterator(): A
}

fun test(notRange: A) {
    for (i in notRange<caret>) {}
}