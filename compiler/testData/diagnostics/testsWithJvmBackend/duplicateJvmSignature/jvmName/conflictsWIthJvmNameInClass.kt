// DIAGNOSTICS: -UNUSED_PARAMETER -INAPPLICABLE_JVM_NAME
// WITH_STDLIB
@JvmName("bar")
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: Any) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun bar(a: Any) {}<!>

class C {
    @JvmName("foo1")
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(list: List<Int>) {}<!>

    @JvmName("foo1")
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(list: List<String>) {}<!>
}

// Conflicts in inheritance.

// A1 -> B1 with accidental override

open class A1 {
    @JvmName("bar")
    open fun foo() {}
}

class B1 : A1() {
    <!ACCIDENTAL_OVERRIDE!>fun bar() {}<!>
}

// A2 -> B2 with intended override and conflicting JVM declarations

open class A2 {
    @JvmName("bar")
    open fun foo() {}
}

class B2 : A2() {
    override fun foo() {}

    <!ACCIDENTAL_OVERRIDE!>fun bar() {}<!>
}

// A3 -> B3 -> C3 with accidental override

open class A3 {
    @JvmName("bar")
    open fun foo() {}
}

open class B3: A3() {
}

class C3: B3() {
    <!ACCIDENTAL_OVERRIDE!>fun bar() {}<!>
}
