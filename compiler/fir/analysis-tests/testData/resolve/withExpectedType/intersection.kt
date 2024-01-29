// LANGUAGE: +ExpectedTypeGuidedResolution

interface A {
    companion object {
        fun foo(): Any { return 1 }
    }
}

interface B {
    companion object {
        fun bar(): Any { return 2 }
    }
}

interface C: B { }

fun handle(test: Any) {
    if (test is A && test is B) {
        when (test) {
            foo() -> 1
            bar() -> 2
            else -> 3
        }
    }

    if (test is C) {
        when (test) {
            <!UNRESOLVED_REFERENCE!>foo<!>() -> 1
            bar() -> 2
            else -> 3
        }
    }
}