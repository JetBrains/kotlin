// WITH_EXTENDED_CHECKERS

class A(b: Boolean) {
    var a: String
    var c: String = ""

    init {
        if (b) {
            a = ""
        } else {
            a = "hello"
            c = a
        }

    }
}