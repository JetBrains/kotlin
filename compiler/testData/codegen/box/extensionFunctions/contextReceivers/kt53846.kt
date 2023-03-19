// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

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
