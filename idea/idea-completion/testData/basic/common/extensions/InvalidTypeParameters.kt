// FIR_COMPARISON
class SomeObject<T, U>() {
    var field : T? = null
}

class A {}
class C {}

fun <T: Comparable<T>, U> SomeObject<T, U>.compareTo(other : SomeObject<T, U>) : Int {
    return 0;
}

fun some() {
    val test = SomeObject<A, A>
    test.<caret>
}

// ABSENT: compareTo