// FIR_IDENTICAL
// SKIP_TXT
// ISSUE: KT-63649

interface Kind
interface System<S : System<S>>

interface Units<
        K : Kind,
        S : System<S>,
        U : Units<K, S, U, M>,
        M : Measure<K, S, U, M>,
        >
    : Comparable<Units<K, S, *, *>>

interface Measure<
        K : Kind,
        S : System<S>,
        U : Units<K, S, U, M>,
        M : Measure<K, S, U, M>,
        >
    : Comparable<Measure<K, S, *, *>>
