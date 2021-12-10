// FILE: Generator.java

import org.jetbrains.annotations.NotNull;

public class Generator {
    @NotNull
    public <T extends Value> T createValue(@NotNull String content) {
        return (T) (new Value(content));
    }
}

// FILE: test.kt
open class Value(val s: String)

val generator = Generator()

val y = generator.createValue("Omega") <!USELESS_CAST!>as Value<!>
