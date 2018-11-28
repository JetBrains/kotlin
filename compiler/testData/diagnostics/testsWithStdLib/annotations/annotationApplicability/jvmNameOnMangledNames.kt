// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class Foo(val x: Int) {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("other")<!>
    fun simple() {}
}

<!INAPPLICABLE_JVM_NAME!>@JvmName("bad")<!>
fun bar(f: Foo) {}

@JvmName("good")
fun baz(r: Result<Int>) {}