// FULL_JDK
// FILE: Call.java

import org.jetbrains.annotations.NotNull;
import java.util.*;

public interface Call<D> {
    @NotNull
    Map<String, String> getArguments();
}

// FILE: test.kt

fun <D : Any> Call<D>.testForEach() {
    arguments.forEach { key, value ->
        key.length
        value.length
    }
    arguments.forEach {
        it.key.length
        it.value.length
    }
}