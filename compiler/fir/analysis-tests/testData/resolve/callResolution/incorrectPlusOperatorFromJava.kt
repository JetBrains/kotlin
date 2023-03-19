// ISSUE: KT-56875
// FILE: Base.java
public class Base {
    public String plus(String... strings) { // (1)
        return "";
    }
}

// FILE: main.kt
operator fun Base.plus(s: String): Int = 0 // (2)

class Derived : Base() {
    fun test_1(x: Base) {
        val y = x + "" // should resolve to (2)
        y.inc() // should be ok
    }

    fun test_2(x: Base) {
        val y = x.plus("") // should resolve to (1)
        y.length // should be ok
    }
}
