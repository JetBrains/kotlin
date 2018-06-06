// !CHECK_TYPE

// FILE: Collections.java
import java.util.List;

public class Collections {
    public static final <T> List<T> emptyList() {
        return null;
    }
}

// FILE: 1.kt

fun bar(): List<String> = null!!

fun test() {
    val f = if (true) {
        Collections.emptyList()
    }
    else {
        bar()
    }

    checkSubtype<List<String>>(f)
}
