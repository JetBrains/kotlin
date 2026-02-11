// LANGUAGE: +ContextReceivers, -ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY

fun box(): String {
    with(Ctx()) {
        Foo()
    }
    return "OK"
}


context(Ctx)
class Foo private constructor(i: Int) {
    constructor() : this(1)
}

class Ctx
