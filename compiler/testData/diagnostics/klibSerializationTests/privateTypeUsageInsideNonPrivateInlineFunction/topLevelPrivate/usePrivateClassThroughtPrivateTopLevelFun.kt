// LANGUAGE: +ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

interface Foo {
    fun foo(): String
}

private class FooImpl : Foo {
    private val ok = "OK"
    override fun foo() = ok
}

private inline fun privateMethod() = FooImpl()

internal inline fun internalMethod(): Foo {
    return <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>privateMethod<!>()<!>
}

fun box(): String {
    return internalMethod().foo()
}
