// MODULE: main
// FILE: main.kt
@JsQualifier("pkg")
external class C {
    fun o(): String

    class D {
        fun k(): String
    }
}

fun box() = C().o() + C.D().k()
