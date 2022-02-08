// FIR_IDENTICAL
// FILE: Provider.java

public class Provider {
    public static Boolean getCondition() {
        return null;
    }
}

// FILE: main.kt

fun test_1(): Int = when (Provider.getCondition()) {
    true -> 1
    false -> 2
}

fun test_2(): Int = when (Provider.getCondition()) {
    true -> 1
    false -> 2
    null -> 3
}
