class A(val next: A? = null) {
    val x: String
    init {
        next?.x = "a"
    }
}

class B(val next: B? = null) {
    var x: String = next?.x ?: "default" // it's ok to use `x` of next
}
