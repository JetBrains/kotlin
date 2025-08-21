// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters


interface ContextParameterTypeA
interface ContextParameterTypeB
interface ReturnType


// top-level callables

context(a: ContextParameterTypeA) fun conflictingTopLevelFunction1(): ReturnType = null!!
context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!

context(b: ContextParameterTypeB) fun conflictingTopLevelFunction2(): ReturnType = null!!
context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingTopLevelFunction2(): ReturnType<!> = null!!

context(a: ContextParameterTypeA) val conflictingTopLevelProperty1: ReturnType get() = null!!
context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingTopLevelProperty1: ReturnType<!> get() = null!!

context(b: ContextParameterTypeB) val conflictingTopLevelProperty2: ReturnType get() = null!!
context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingTopLevelProperty2: ReturnType<!> get() = null!!

// top-level callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterTypeA) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterTypeA) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(a: ContextParameterTypeA, b: ContextParameterTypeB) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

// top-level callables (in different c-level sets)

context(a: ContextParameterTypeA) fun validIdentifier1a(): ReturnType = null!!
context(a: ContextParameterTypeA, b: ContextParameterTypeB) val validIdentifier1a: ReturnType get() = null!!

context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun validIdentifier1b(): ReturnType = null!!
context(a: ContextParameterTypeA) val validIdentifier1b: ReturnType get() = null!!


// member callables

class ReceiverType {
    context(a: ContextParameterTypeA) fun conflictingMemberFunction1(): ReturnType = null!!
    context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingMemberFunction1(): ReturnType<!> = null!!

    context(a: ContextParameterTypeA) val conflictingMemberProperty1: ReturnType get() = null!!
    context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingMemberProperty1: ReturnType<!> get() = null!!

    // member callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(a: ContextParameterTypeA) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(a: ContextParameterTypeA) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    context(a: ContextParameterTypeA, b: ContextParameterTypeB) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    // member callables (in different c-level sets)

    context(a: ContextParameterTypeA) fun validIdentifier1a(): ReturnType = null!!
    context(a: ContextParameterTypeA, b: ContextParameterTypeB) val validIdentifier1a: ReturnType get() = null!!

    context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun validIdentifier1b(): ReturnType = null!!
    context(a: ContextParameterTypeA) val validIdentifier1b: ReturnType get() = null!!
}


// extension callables

context(a: ContextParameterTypeA) fun ReceiverType.conflictingExtensionFunction1(): ReturnType = null!!
context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!

context(a: ContextParameterTypeA) val ReceiverType.conflictingExtensionProperty1: ReturnType get() = null!!
context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val ReceiverType.conflictingExtensionProperty1: ReturnType<!> get() = null!!

// extension callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterTypeA) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterTypeA) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(a: ContextParameterTypeA, b: ContextParameterTypeB) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

// extension callables (in different c-level sets)

context(a: ContextParameterTypeA) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1a<!>(): ReturnType = null!!
context(a: ContextParameterTypeA, b: ContextParameterTypeB) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1a<!>: ReturnType get() = null!!

context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1b<!>(): ReturnType = null!!
context(a: ContextParameterTypeA) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1b<!>: ReturnType get() = null!!


// local callables

fun localScope() {
    context(a: ContextParameterTypeA) fun conflictingLocalFunction1(): ReturnType = null!!
    context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingLocalFunction1(): ReturnType<!> = null!!

    // local callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(a: ContextParameterTypeA) fun validLocalFunctionViaHidingDeprecation1(): ReturnType = null!!

    context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun validLocalFunctionViaHidingDeprecation1(): ReturnType<!> = null!!
}


/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, interfaceDeclaration, localFunction, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral */
