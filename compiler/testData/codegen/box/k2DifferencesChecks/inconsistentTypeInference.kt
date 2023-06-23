// ORIGINAL: /compiler/testData/diagnostics/testsWithStdLib/builderInference/inconsistentTypeInference.fir.kt
// WITH_STDLIB
// !RENDER_DIAGNOSTICS_FULL_TEXT


fun foo() {
    buildList {
        add("Boom")
        println(plus(1)[0])
    }
}



fun box() = "OK".also { foo() }
