// FIR_IDENTICAL
// ISSUE: KT-67264
// WITH_STDLIB

// FILE: MyList.java
public class MyList<T> {
    void addAll(Iterable<? extends T> elements) {
    }
}

// FILE: main.kt
fun foo() {
    val anys = listOf(Any(), Any(), Any())
    MyList<suspend () -> Any>().addAll(
        anys.map { any ->
            {
                any
            }
        }
    )
}
