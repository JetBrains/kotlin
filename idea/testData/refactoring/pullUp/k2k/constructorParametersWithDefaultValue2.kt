open class A(n: Int, s: String = "") {

}

class <caret>B(
        // INFO: {"checked": "true"}
        val i: Int
) : A(1)