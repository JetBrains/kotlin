// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-29890
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE_FEATURE_TOGGLED: ConsiderLambdaArrayConstructorsInlinableInBodiesOfInlineFunctions

inline fun testConstructor1(size: Int, init: (Int) -> Double) = DoubleArray(size, <!USAGE_IS_NOT_INLINABLE!>init<!>)
inline fun testConstructor2(size: Int, init: (Int) -> Double) = DoubleArray(size) { init(it) }

inline fun testConstructor3(size: Int, init: (Int) -> Double) = Array(size) { DoubleArray(size) { init(it) } }
inline fun testConstructor4(size: Int, init: (Int) -> Double) = Array(size) { DoubleArray(size, <!USAGE_IS_NOT_INLINABLE!>init<!>) }

typealias MyDoubleArray = Array<Double>

inline fun testConstructor5(size: Int, init: (Int) -> Double) = MyDoubleArray(size, <!USAGE_IS_NOT_INLINABLE!>init<!>)
inline fun testConstructor6(size: Int, init: (Int) -> Double) = MyDoubleArray(size) { init(it) }

inline fun testConstructor7(size: Int, crossinline init: (Int) -> Double) = DoubleArray(size, <!USAGE_IS_NOT_INLINABLE!>init<!>)
inline fun testConstructor8(size: Int, crossinline init: (Int) -> Double) = DoubleArray(size) { init(it) }

inline fun testConstructor9(size: Int, noinline init: (Int) -> Double) = DoubleArray(size, init)
inline fun testConstructor10(size: Int, noinline init: (Int) -> Double) = DoubleArray(size) { init(it) }

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, lambdaLiteral */
