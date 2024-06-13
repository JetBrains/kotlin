fun call {
    val foo = CInt32VarX<Int>()
    <expr>++foo.value</expr>
}

class CInt32VarX<T>

var <T : Int> CInt32VarX<T>.value: T
    get() = TODO()
    set(value) {}
