// FIR_IDENTICAL
// !LANGUAGE: +NoAdditionalErrorsInK1DiagnosticReporter

fun foo() {
    buildList {
        add("Boom")
        println(plus(1)[0])
    }
}
