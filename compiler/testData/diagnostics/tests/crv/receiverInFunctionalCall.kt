// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// WITH_STDLIB
@file:MustUseReturnValues

fun baz() = 123
fun nullableBaz(): Int? = null

fun Int.extensionFun() { }
val Int.extensionProperty: Unit
    get() = Unit

fun <T> T.extensionFunGeneric() { }
val <T> T.extensionPropertyGeneric: Unit
    get() = Unit

context(a: Int)
fun Int.contextFunWithExtension() {}

context(a: Int)
val Int.contextPropertyWithExtension: Unit
    get() = Unit

fun receiverUsage() {
    baz().extensionFun()
    nullableBaz()?.extensionFun()
    baz().extensionProperty
    nullableBaz()?.extensionProperty

    baz().extensionFunGeneric()
    nullableBaz()?.extensionFunGeneric()
    baz().extensionPropertyGeneric
    nullableBaz()?.extensionPropertyGeneric

    context(baz()) {
        baz().contextFunWithExtension()
        nullableBaz()?.contextFunWithExtension()
        baz().contextPropertyWithExtension
        nullableBaz()?.contextPropertyWithExtension
        Unit
    }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, integerLiteral, lambdaLiteral, nullableType, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, safeCall, typeParameter */
