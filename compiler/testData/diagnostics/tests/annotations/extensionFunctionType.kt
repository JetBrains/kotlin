// This test checks that annotations on extension function types are preserved. See the corresponding .txt file

annotation class ann

interface Some {
    fun f1(): String.() -> Int
    fun f2(): <!UNSUPPORTED!>@Extension<!> String.() -> Int
    fun f3(): <!UNSUPPORTED!>@ann<!> String.() -> Int
    fun f4(): <!UNSUPPORTED!>@Extension<!> <!UNSUPPORTED!>@ann<!> String.() -> Int
}