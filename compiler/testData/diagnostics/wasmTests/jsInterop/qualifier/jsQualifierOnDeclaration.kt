// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: topLevelExternal.kt
package topLevelExternal

@JsQualifier("a")
external fun f(): Int

@JsQualifier("a")
external val x: Int

@JsQualifier("a")
external var y: Int

@JsQualifier("a")
external class C {
    @JsQualifier("b")
    fun member(): Int

    @JsQualifier("b")
    val memberProperty: Int

    @JsQualifier("b")
    class Nested

    @JsQualifier("b")
    inner class <!WRONG_EXTERNAL_DECLARATION!>Inner<!>

    @JsQualifier("b")
    interface NestedInterface

    @JsQualifier("b")
    companion object {
        @JsQualifier("c")
        fun companionMember(): Int
    }
}

@JsQualifier("a")
external object O {
    @JsQualifier("b")
    fun member(): Int

    @JsQualifier("b")
    interface NestedInterface
}

@JsQualifier("a")
external interface I {
    @JsQualifier("b")
    fun member(): Int
}

// FILE: nonExternal.kt
package nonExternal

@JsQualifier("a")
fun f(): Int = 1

@JsQualifier("a")
val x: Int = 1

@JsQualifier("a")
class C(@property:JsQualifier("b") val p: Int) {
    @JsQualifier("b")
    fun member(): Int = 1

    @JsQualifier("b")
    inner class Inner

    @JsQualifier("b")
    interface NestedInterface
}

@JsQualifier("a")
object O

@JsQualifier("a")
interface I {
    @JsQualifier("b")
    interface NestedInterface
}

fun local() {
    @JsQualifier("a")
    fun localFun(): Int = 1

    @JsQualifier("a")
    class LocalClass
}

// FILE: typealiases.kt
package typealiases

typealias JsQ = JsQualifier
typealias JsQ2 = JsQ

@JsQ("a")
external fun f(): Int

@JsQ2("a")
external val x: Int

@JsQ(<!WRONG_JS_QUALIFIER!>"a..b"<!>)
external fun wrongQualifier(): Int

external class C {
    @JsQ("b")
    fun member(): Int
}

@JsQ("a")
fun nonExternal(): Int = 1

@JsQ2("a")
class NonExternal

// FILE: typealiasOnFile.kt
@file:typealiases.JsQ("q")
package typealiasOnFile

external fun ok(): Int

<!NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE!>fun nonExternal(): Int<!> = 1
