// LANGUAGE: -ProhibitJsQualifierOnMembersAndNonExternals
// ^^^ Pre-KT-82785 behavior is not broken.
// ES_MODULES
// FILE: jsQualifierOnLocalDeclarations.mjs
export class C {
    o() { return "O" }
}

C.D = class D {
    k() { return "K" }
}

// FILE: lib1.kt
@file:JsModule("./jsQualifierOnLocalDeclarations.mjs")

external class C {
    @JsQualifier("a")
    fun o(): String

    @JsQualifier("b")
    class D {
        fun k(): String
    }
}

// FILE: main.kt
fun box() = C().o() + C.D().k()
