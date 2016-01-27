// "Change 'A' function return type to 'B'" "false"
// ACTION: Change 'b' type to 'A'
// ACTION: Convert property initializer to getter
// ERROR: Type mismatch: inferred type is A but B was expected

class A constructor() {}
interface B

val b: B = <caret>A()