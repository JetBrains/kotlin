// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ExpectedTypeGuidedResolution

enum class Sample {
    FIRST,
    SECOND;
}

fun trivial1(message: String): Sample {
    return <!UNRESOLVED_REFERENCE!>FIRST<!>.takeIf { message == "hello" } ?: SECOND
}

fun trivial2(message: String): Sample {
    return Sample.FIRST.takeIf { message == "hello" } ?: SECOND
}

fun trivial3(): Sample {
    return run { <!UNRESOLVED_REFERENCE!>FIRST<!> }
}