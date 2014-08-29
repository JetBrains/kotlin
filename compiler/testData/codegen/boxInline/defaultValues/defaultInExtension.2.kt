package test

inline public fun String.run(p1: String? = null): String {
    return this + p1
}

inline public fun String.run(p1: String = "", lambda: (a: String, b: Int) -> String, p2: Int = 0): String {
    return lambda(p1, p2) + this
}

public class Z(val value: Int = 0) {

    inline public fun String.run(p1: String? = null): String? {
        return this + p1
    }

    inline public fun String.run(p1: String = "", lambda: (a: String, b: Int) -> String, p2: Int = 0): String {
        return lambda(p1, p2) + this
    }

}