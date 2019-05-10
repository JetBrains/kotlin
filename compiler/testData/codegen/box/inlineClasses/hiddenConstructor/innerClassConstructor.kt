// !LANGUAGE: +InlineClasses

inline class S(val string: String)

class Outer(val s1: S) {
    inner class Inner(val s2: S) {
        val test = s1.string + s2.string
    }
}

fun box() = Outer(S("O")).Inner(S("K")).test