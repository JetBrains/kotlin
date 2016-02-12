// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in B.<init>
// PARAM_DESCRIPTOR: value-parameter b: kotlin.Int defined in B.<init>
interface T

class A(a: Int, b: Int): T

class B(a: Int, b: Int): T by A(<selection>a + b</selection>, a - b)