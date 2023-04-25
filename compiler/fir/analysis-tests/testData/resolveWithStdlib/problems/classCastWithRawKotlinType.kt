// ISSUE: KT-56630
// FILE: Usage.java

public class Usage extends Apple {

}

// FILE: test.kt

interface Inter
open class Apple<T : Inter> : Inter
class XXX : Usage()

fun main() {
    println(XXX())
}
