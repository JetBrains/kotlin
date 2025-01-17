// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: strict
// JSPECIFY_STATE: strict

// FILE: api/package-info.java
@org.jspecify.annotations.NullMarked
package api;

// FILE: api/Transformer.java
package api;

public interface Transformer<OUT extends @org.jspecify.annotations.Nullable Object, IN> {
    OUT transform(IN in);
}

// FILE: api/Consumer.java
package api;

public class Consumer {
    public Consumer applyNullable(Transformer<@org.jspecify.annotations.Nullable String, String> transformer) { return this; }
    public Consumer applyNotNull(Transformer<String, String> transformer) { return this; }
}

// FILE: main.kt

import api.*

fun main() {
    Consumer().applyNullable { null } // should accept null
    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    Consumer().applyNotNull { <!NULL_FOR_NONNULL_TYPE, NULL_FOR_NONNULL_TYPE!>null<!> } // expected nullness error
}
