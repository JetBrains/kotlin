// "Make 'SealedClass' public" "true"
// ACTION: "Make 'Test' private"

private sealed class SealedClass

class Test : <caret>SealedClass()