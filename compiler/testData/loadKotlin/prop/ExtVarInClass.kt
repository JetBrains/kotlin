package test

class ExtPropInClass {
    var Int.itIs: Int
        get() = throw Exception()
        set(p: Int) = throw Exception()
}
