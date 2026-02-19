interface Interface {
    var a: String
}

open class Open {
    val a: String = "default"
}

class Impl : Open(), Interface
