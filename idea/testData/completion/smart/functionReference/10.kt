class C {
    fun foo(s: String): Boolean = true

    fun bar() {
        listOf("a").filter(<caret>)
    }

    default object {
        fun staticFoo(s: String): Boolean = true
    }
}

fun C.extensionFoo(s: String): Boolean = true
fun globalFoo(s: String): Boolean = true

// ABSENT: ::foo
// ABSENT: ::staticFoo
// ABSENT: ::extensionFoo
// EXIST: ::globalFoo
