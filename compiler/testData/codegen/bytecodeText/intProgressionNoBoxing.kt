fun foo() {
    for (i in 1..2 step 4) {}
}

// JVM non-IR does NOT specifically handle "step" progressions. The stepped progression in the above code are constructed and its
// first/last/step properties are retrieved.
// JVM IR has an optimized handler for "step" progressions and elides the construction of the stepped progressions.

// JVM_TEMPLATES
// 1 INVOKEVIRTUAL kotlin/ranges/IntProgression.getFirst \(\)I
// 1 getFirst
// 1 INVOKEVIRTUAL kotlin/ranges/IntProgression.getLast \(\)I
// 1 getLast
// 1 INVOKEVIRTUAL kotlin/ranges/IntProgression.getStep \(\)I
// 1 getStep

// JVM_IR_TEMPLATES
// 0 getFirst
// 0 getLast
// 0 getStep