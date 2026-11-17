// LANGUAGE: +WasmAllowCompanionObjectInExternalInterface
// ISSUE: KT-76462
// FILE: externalInterfaceNestedCompanion.js

const Outer = {
    Inner: {
        x: "inner companion"
    }
};

class NestedClass {
    constructor(s) { this.s = s; }
}

const Statics = {
    Holder: {
        y: "object in companion"
    },
    NestedClass: NestedClass
};

// FILE: externalInterfaceNestedCompanion.kt

// A companion object of a nested interface is qualified by all enclosing names: `Outer.Inner`.
external interface Outer {
    interface Inner {
        companion object {
            val x: String
        }
    }
}

// Declarations nested in a companion object are qualified by the interface name in JS: `Statics.Holder`, `Statics.NestedClass`.
external interface Statics {
    companion object {
        object Holder {
            val y: String
        }

        class NestedClass(s: String) {
            val s: String
        }
    }
}

fun box(): String {
    if (Outer.Inner.x != "inner companion") return "Fail 1: ${Outer.Inner.x}"
    if (Statics.Companion.Holder.y != "object in companion") return "Fail 2: ${Statics.Companion.Holder.y}"
    val nested = Statics.Companion.NestedClass("nested class")
    if (nested.s != "nested class") return "Fail 3: ${nested.s}"
    return "OK"
}
