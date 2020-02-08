// "Convert to anonymous object" "true"
interface B<X, Y, Z> {
    fun bar(a: Z, b: Y): X
}

val b = <caret>B { a: Int, b: Long -> "" }