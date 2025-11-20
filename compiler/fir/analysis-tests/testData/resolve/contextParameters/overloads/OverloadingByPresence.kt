// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters


interface ContextParameterType
interface ReturnType


// top-level callables

fun validTopLevelFunction(): ReturnType = null!!
context(ctx: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun validTopLevelFunction(): ReturnType<!> = null!!

val validTopLevelProperty: ReturnType get() = null!!
context(ctx: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val validTopLevelProperty: ReturnType<!> get() = null!!


// member callables

class ReceiverType {
    fun validMemberFunction(): ReturnType = null!!
    context(ctx: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun validMemberFunction(): ReturnType<!> = null!!

    val validMemberProperty: ReturnType get() = null!!
    context(ctx: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val validMemberProperty: ReturnType<!> get() = null!!
}


// extension callables

fun ReceiverType.validExtensionFunction(): ReturnType = null!!
context(ctx: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun ReceiverType.validExtensionFunction(): ReturnType<!> = null!!

val ReceiverType.validExtensionProperty: ReturnType get() = null!!
context(ctx: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val ReceiverType.validExtensionProperty: ReturnType<!> get() = null!!


// local callables

fun localScope() {
    fun validLocalFunction(): ReturnType = null!!
    context(ctx: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun validLocalFunction(): ReturnType<!> = null!!
}


/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, interfaceDeclaration, localFunction, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver */
