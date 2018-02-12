expect class C {
    constructor(arg: Any?)
}

actual data class C actual constructor(val arg: Any?) {

}

expect class T
actual typealias T = C

expect fun f(arg: Int): Int
actual fun f(arg: Int) = arg

expect var p: String
actual var p: String = "p"
