// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-53846
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
