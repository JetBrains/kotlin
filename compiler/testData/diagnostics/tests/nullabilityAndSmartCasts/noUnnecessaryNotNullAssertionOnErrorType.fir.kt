package a

fun foo() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar()!!<!>
}

fun bar() = <!UNRESOLVED_REFERENCE!>aa<!>
