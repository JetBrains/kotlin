package some

annotation class SomeAnnotation

class Complete(@set:Some<caret> var field: Int)

// ELEMENT: SomeAnnotation
// FIR_COMPARISON