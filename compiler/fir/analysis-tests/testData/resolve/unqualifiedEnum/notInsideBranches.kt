// LANGUAGE: +ExpectedTypeGuidedResolution

enum class Some {
    FIRST,
    SECOND;
}

fun foo(s: Some) = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>when (s) {
    _.FIRST -> <!UNRESOLVED_REFERENCE!>_.SECOND<!>
    _.SECOND -> <!UNRESOLVED_REFERENCE!>_.FIRST<!>
}<!>
