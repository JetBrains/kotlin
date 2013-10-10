fun test() {
    val a = if (true) {
        val x = 1
        ({ x })
    } else {
        { 2 }
    }
    TypeOf(a): TypeOf<Function0<Int>>
}

class TypeOf<T>(t: T)