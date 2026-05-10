// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters


interface ContextParameterSuperType
interface ContextParameterSubType: ContextParameterSuperType
interface ContextParameterType
interface ReturnType


// top-level callables

context(old: ContextParameterSuperType) fun conflictingTopLevelFunction1(): ReturnType = null!!
context(old: ContextParameterSubType, new: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!

context(old: ContextParameterSuperType) fun conflictingTopLevelFunction2(): ReturnType = null!!
context(new: ContextParameterType, old: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingTopLevelFunction2(): ReturnType<!> = null!!

context(old: ContextParameterSuperType) val conflictingTopLevelProperty1: ReturnType get() = null!!
context(old: ContextParameterSubType, new: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingTopLevelProperty1: ReturnType<!> get() = null!!

context(old: ContextParameterSuperType) val conflictingTopLevelProperty2: ReturnType get() = null!!
context(new: ContextParameterType, old: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingTopLevelProperty2: ReturnType<!> get() = null!!

// top-level callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(old: ContextParameterSuperType) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

context(old: ContextParameterSubType, new: ContextParameterType) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(old: ContextParameterSuperType) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(old: ContextParameterSubType, new: ContextParameterType) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

// top-level callables (in different c-level sets)

context(old: ContextParameterSuperType) fun validIdentifier1a(): ReturnType = null!!
context(old: ContextParameterSubType, new: ContextParameterType) val validIdentifier1a: ReturnType get() = null!!

context(old: ContextParameterSubType, new: ContextParameterType) fun validIdentifier1b(): ReturnType = null!!
context(old: ContextParameterSuperType) val validIdentifier1b: ReturnType get() = null!!


// member callables

class ReceiverType {
    context(old: ContextParameterSuperType) fun conflictingMemberFunction1(): ReturnType = null!!
    context(old: ContextParameterSubType, new: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingMemberFunction1(): ReturnType<!> = null!!

    context(old: ContextParameterSuperType) val conflictingMemberProperty1: ReturnType get() = null!!
    context(old: ContextParameterSubType, new: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingMemberProperty1: ReturnType<!> get() = null!!

    // member callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(old: ContextParameterSuperType) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    context(old: ContextParameterSubType, new: ContextParameterType) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(old: ContextParameterSuperType) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    context(old: ContextParameterSubType, new: ContextParameterType) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    // member callables (in different c-level sets)

    context(old: ContextParameterSuperType) fun validIdentifier1a(): ReturnType = null!!
    context(old: ContextParameterSubType, new: ContextParameterType) val validIdentifier1a: ReturnType get() = null!!

    context(old: ContextParameterSubType, new: ContextParameterType) fun validIdentifier1b(): ReturnType = null!!
    context(old: ContextParameterSuperType) val validIdentifier1b: ReturnType get() = null!!
}


// extension callables

context(old: ContextParameterSuperType) fun ReceiverType.conflictingExtensionFunction1(): ReturnType = null!!
context(old: ContextParameterSubType, new: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!

context(old: ContextParameterSuperType) val ReceiverType.conflictingExtensionProperty1: ReturnType get() = null!!
context(old: ContextParameterSubType, new: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val ReceiverType.conflictingExtensionProperty1: ReturnType<!> get() = null!!

// extension callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(old: ContextParameterSuperType) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

context(old: ContextParameterSubType, new: ContextParameterType) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(old: ContextParameterSuperType) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(old: ContextParameterSubType, new: ContextParameterType) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

// extension callables (in different c-level sets)

context(old: ContextParameterSuperType) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1a<!>(): ReturnType = null!!
context(old: ContextParameterSubType, new: ContextParameterType) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1a<!>: ReturnType get() = null!!

context(old: ContextParameterSubType, new: ContextParameterType) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1b<!>(): ReturnType = null!!
context(old: ContextParameterSuperType) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1b<!>: ReturnType get() = null!!


// local callables

fun localScope() {
    context(old: ContextParameterSuperType) fun conflictingLocalFunction1(): ReturnType = null!!
    context(old: ContextParameterSubType, new: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingLocalFunction1(): ReturnType<!> = null!!

    // local callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(old: ContextParameterSuperType) fun validLocalFunctionViaHidingDeprecation1(): ReturnType = null!!

    context(old: ContextParameterSubType, new: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun validLocalFunctionViaHidingDeprecation1(): ReturnType<!> = null!!
}


/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, interfaceDeclaration, localFunction, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral */
