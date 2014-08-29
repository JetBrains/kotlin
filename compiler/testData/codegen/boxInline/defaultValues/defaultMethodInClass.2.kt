package test

public class Z(public val value: Int = 0) {

    inline public fun run(p1: String? = null): String? {
        return p1 + value
    }


    inline public fun run(p1: String = "", lambda: (a: String, b: Int) -> String, p2: Int = 0): String {
        return lambda(p1, p2)
    }
}