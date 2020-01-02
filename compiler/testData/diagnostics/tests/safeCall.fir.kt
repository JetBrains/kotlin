// !WITH_NEW_INFERENCE

fun f(s: String, action: (String.() -> Unit)?) {
    s.foo().bar().<!INAPPLICABLE_CANDIDATE!>action<!>()
}

fun String.foo() = ""

fun String.bar() = ""

// --------------------------------------------------------

val functions: Map<String, () -> Any> = TODO()

fun run(name: String) = <!INAPPLICABLE_CANDIDATE!>functions[name]()<!>