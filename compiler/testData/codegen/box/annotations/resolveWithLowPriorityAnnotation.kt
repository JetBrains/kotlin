// WITH_STDLIB

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun foo(i: Int) = 1

fun foo(a: Any) = 2

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun bar(a: String?) = 3

fun bar(a: Any) = 4

class MyString(val value: String)

class Baz
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
constructor(val s: String) {
    constructor(s: MyString): this(s.value)
}

fun Baz(s: String) = Baz(MyString(s + "!"))

fun box(): String {
    if (foo(1) != 2) return "fail1"
    if (bar(null) != 3) return "fail2"
    if (Baz("hello").s != "hello!") return "fail3"
    return "OK"
}
