// PARAM_TYPES: T
// PARAM_DESCRIPTOR: value-parameter t: T defined in B.<init>
interface T

class A(a: Int, b: Int): T

class B(t: T): T by <selection>t</selection>