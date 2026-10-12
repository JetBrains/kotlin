// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class C {
    <!WRONG_EXTERNAL_DECLARATION!>@nativeSetter
    fun Int.ext(a: String, v: Int)<!>

    class Nested {
        <!WRONG_EXTERNAL_DECLARATION!>@nativeSetter
        fun Int.ext(a: String, v: Int)<!>
    }

    companion object {
        <!WRONG_EXTERNAL_DECLARATION!>@nativeSetter
        fun Int.ext(a: String, v: Int)<!>
    }
}

external interface I {
    <!WRONG_EXTERNAL_DECLARATION!>@nativeSetter
    fun Int.ext(a: String, v: Int)<!>
}

external object O {
    <!WRONG_EXTERNAL_DECLARATION!>@nativeSetter
    fun Int.ext(a: String, v: Int)<!>
}
