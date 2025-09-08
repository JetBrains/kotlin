// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters


interface ContextParameterType
interface ReturnType


// top-level callables

context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!
context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!

context(ctx: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty1<!>: ReturnType get() = null!!
context(ctx: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty1<!>: ReturnType get() = null!!

// top-level callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterType) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

context(ctx: ContextParameterType) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterType) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(ctx: ContextParameterType) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

// top-level callables (in different c-level sets)

context(ctx: ContextParameterType) fun validIdentifier1(): ReturnType = null!!
context(ctx: ContextParameterType) val validIdentifier1: ReturnType get() = null!!


// member callables

class ReceiverType {
    context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingMemberFunction1(): ReturnType<!> = null!!
    context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingMemberFunction1(): ReturnType<!> = null!!

    context(ctx: ContextParameterType) val <!REDECLARATION!>conflictingMemberProperty1<!>: ReturnType get() = null!!
    context(ctx: ContextParameterType) val <!REDECLARATION!>conflictingMemberProperty1<!>: ReturnType get() = null!!

    // member callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterType) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    context(ctx: ContextParameterType) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterType) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    context(ctx: ContextParameterType) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    // member callables (in different c-level sets)

    context(ctx: ContextParameterType) fun validIdentifier1(): ReturnType = null!!
    context(ctx: ContextParameterType) val validIdentifier1: ReturnType get() = null!!
}


// extension callables

context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!
context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!

context(ctx: ContextParameterType) val ReceiverType.<!REDECLARATION!>conflictingExtensionProperty1<!>: ReturnType get() = null!!
context(ctx: ContextParameterType) val ReceiverType.<!REDECLARATION!>conflictingExtensionProperty1<!>: ReturnType get() = null!!

// extension callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterType) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

context(ctx: ContextParameterType) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterType) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(ctx: ContextParameterType) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

// extension callables (in different c-level sets)

context(ctx: ContextParameterType) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1<!>(): ReturnType = null!!
context(ctx: ContextParameterType) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1<!>: ReturnType get() = null!!


// local callables

fun localScope() {
    context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingLocalFunction1(): ReturnType<!> = null!!
    context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingLocalFunction1(): ReturnType<!> = null!!

    // local callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun validLocalFunctionViaHidingDeprecation1(): ReturnType<!> = null!!

    context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun validLocalFunctionViaHidingDeprecation1(): ReturnType<!> = null!!

    // local callables (in different c-level sets)

    context(ctx: ContextParameterType) fun validIdentifier1(): ReturnType = null!!
}


/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, interfaceDeclaration, localFunction, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral */
