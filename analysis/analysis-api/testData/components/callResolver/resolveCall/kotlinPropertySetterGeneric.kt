fun call {
    val foo = CInt32VarX<Int>()
    foo.<expr>value</expr> = 42
}

class CInt32VarX<T>

var <T : Int> CInt32VarX<T>.value: T
    get() = TODO()
    set(value) {}
