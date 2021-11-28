// !DIAGNOSTICS: -UNUSED_PARAMETER
@JvmName("bar")
fun foo(a: Any) {}

fun bar(a: Any) {}

class C {
    @JvmName("foo1")
    fun foo(list: List<Int>) {}

    @JvmName("foo1")
    fun foo(list: List<String>) {}
}

// Conflicts in inheritance.

// A1 -> B1 with accidental override

open class A1 {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("bar")<!>
    open fun foo() {}
}

class B1 : A1() {
    fun bar() {}
}

// A2 -> B2 with intended override and conflicting JVM declarations

open class A2 {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("bar")<!>
    open fun foo() {}
}

class B2 : A2() {
    override fun foo() {}

    fun bar() {}
}

// A3 -> B3 -> C3 with accidental override

open class A3 {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("bar")<!>
    open fun foo() {}
}

open class B3: A3() {
}

class C3: B3() {
    fun bar() {}
}
