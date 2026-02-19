// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// DIAGNOSTICS: -NOTHING_TO_INLINE

interface Foo {
    fun foo(): String
}

private class FooImpl : Foo {
    private val ok = "OK"
    override fun foo() = ok
}

private inline fun privateMethod() = FooImpl()

internal inline fun internalMethod(): Foo {
    return <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>privateMethod<!>()
}

fun box(): String {
    return internalMethod().foo()
}
