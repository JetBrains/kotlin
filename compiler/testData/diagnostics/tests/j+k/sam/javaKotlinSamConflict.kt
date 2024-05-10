// FIR_IDENTICAL
// ISSUE: KT-48323

// FILE: Jerry.java
public interface Jerry {
    void call(int i);
}

// FILE: Tom.kt
fun interface Tom {
    fun tom(i: Int)
}

fun foo(m: Tom) = 1
fun foo(j: Jerry) = "2"
fun test() {
    val result = foo { i ->
        val j = i + 1
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
}
