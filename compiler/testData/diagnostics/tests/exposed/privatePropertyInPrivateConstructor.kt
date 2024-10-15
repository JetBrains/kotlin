// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
private enum class Foo { A, B }

class Bar private constructor(private val foo: Foo)