fun foo() {
    for (i in 1..2 step 4) {}
}

// JVM IR has an optimized handler for "step" progressions and elides the construction of the stepped progressions.
// 0 getFirst
// 0 getLast
// 0 getStep
