// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class C {
    <!WRONG_EXTERNAL_DECLARATION!>@nativeGetter
    fun Int.ext(a: String): Int?<!>

    class Nested {
        <!WRONG_EXTERNAL_DECLARATION!>@nativeGetter
        fun Int.ext(a: String): Int?<!>
    }

    companion object {
        <!WRONG_EXTERNAL_DECLARATION!>@nativeGetter
        fun Int.ext(a: String): Int?<!>
    }
}

external interface I {
    <!WRONG_EXTERNAL_DECLARATION!>@nativeGetter
    fun Int.ext(a: String): Int?<!>
}

external object O {
    <!WRONG_EXTERNAL_DECLARATION!>@nativeGetter
    fun Int.ext(a: String): Int?<!>
}
