// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

fun box(): String {
    with(Ctx()) {
        Foo(1)
    }
    return "OK"
}

context(Ctx)
class Foo constructor(
    i: Int
) {
    constructor() : this(1)
}

class Ctx