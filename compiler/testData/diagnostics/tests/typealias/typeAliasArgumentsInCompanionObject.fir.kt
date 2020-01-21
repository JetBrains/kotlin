class C<T1, T2> {
    companion object {
        val OK = "OK"
    }
}

typealias C2<T> = C<T, T>

val test1: String = C2<String>.OK
val test2: String = C2.OK
