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
    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("b")<!>
    fun member(): Int

    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("b")<!>
    val memberProperty: Int

    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("b")<!>
    class Nested

    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("b")<!>
    inner class <!WRONG_EXTERNAL_DECLARATION!>Inner<!>

    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("b")<!>
    interface NestedInterface

    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("b")<!>
    companion object {
        <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("c")<!>
        fun companionMember(): Int
    }
}

@JsQualifier("a")
external object O {
    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("b")<!>
    fun member(): Int

    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("b")<!>
    interface NestedInterface
}

@JsQualifier("a")
external interface I {
    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("b")<!>
    fun member(): Int
}

// FILE: nonExternal.kt
package nonExternal

<!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("a")<!>
fun f(): Int = 1

<!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("a")<!>
val x: Int = 1

<!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("a")<!>
class C(<!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@property:JsQualifier("b")<!> val p: Int) {
    <!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("b")<!>
    fun member(): Int = 1

    <!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("b")<!>
    inner class Inner

    <!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("b")<!>
    interface NestedInterface
}

<!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("a")<!>
object O

<!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("a")<!>
interface I {
    <!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("b")<!>
    interface NestedInterface
}

fun local() {
    <!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("a")<!>
    fun localFun(): Int = 1

    <!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQualifier("a")<!>
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
    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQ("b")<!>
    fun member(): Int
}

<!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQ("a")<!>
fun nonExternal(): Int = 1

<!JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL!>@JsQ2("a")<!>
class NonExternal

// FILE: typealiasOnFile.kt
@file:typealiases.JsQ("q")
package typealiasOnFile

external fun ok(): Int

<!NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE!>fun nonExternal(): Int<!> = 1
