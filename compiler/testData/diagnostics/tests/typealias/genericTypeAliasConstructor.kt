class C<T>(val x: T, val y: String) {
    constructor(x: T): this(x, "")
}

typealias GTC<T> = C<T>

val test1 = GTC<String>("", "")
val test2 = GTC<String>("", "")
val test3 = GTC<String>("")
val test4 = GTC<String>("")
