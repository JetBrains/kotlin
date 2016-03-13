// !DIAGNOSTICS: -UNUSED_PARAMETER
<!CONFLICTING_JVM_DECLARATIONS!>@JvmName("bar")
fun foo(a: Any)<!> {}

<!CONFLICTING_JVM_DECLARATIONS!>fun bar(a: Any)<!> {}

class C {
    <!CONFLICTING_JVM_DECLARATIONS!>@JvmName("foo1")
    fun foo(list: List<Int>)<!> {}

    <!CONFLICTING_JVM_DECLARATIONS!>@JvmName("foo1")
    fun foo(list: List<String>)<!> {}
}

// Conflicts in inheritance.

// A1 -> B1 with accidental override

open class A1 {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("bar")<!>
    open fun foo() {}
}

class B1 : A1() {
    <!ACCIDENTAL_OVERRIDE!>fun bar()<!> {}
}

// A2 -> B2 with intended override and conflicting JVM declarations

open class A2 {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("bar")<!>
    open fun foo() {}
}

class <!CONFLICTING_JVM_DECLARATIONS!>B2<!> : A2() {
    override fun foo() {}

    <!CONFLICTING_JVM_DECLARATIONS!>fun bar()<!> {}
}

// A3 -> B3 -> C3 with accidental override

open class A3 {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("bar")<!>
    open fun foo() {}
}

open class B3: A3() {
}

class C3: B3() {
    <!ACCIDENTAL_OVERRIDE!>fun bar()<!> {}
}