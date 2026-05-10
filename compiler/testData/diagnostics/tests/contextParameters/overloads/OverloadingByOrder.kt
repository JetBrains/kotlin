// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters


interface ContextParameterTypeA
interface ContextParameterTypeB
interface ReturnType


// top-level callables

context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!
context(b: ContextParameterTypeB, a: ContextParameterTypeA) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!

context(a: ContextParameterTypeA, b: ContextParameterTypeB) val <!REDECLARATION!>conflictingTopLevelProperty1<!>: ReturnType get() = null!!
context(b: ContextParameterTypeB, a: ContextParameterTypeA) val <!REDECLARATION!>conflictingTopLevelProperty1<!>: ReturnType get() = null!!

// top-level callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

context(b: ContextParameterTypeB, a: ContextParameterTypeA) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterTypeA, b: ContextParameterTypeB) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(b: ContextParameterTypeB, a: ContextParameterTypeA) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

// top-level callables (in different c-level sets)

context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun validIdentifier1(): ReturnType = null!!
context(b: ContextParameterTypeB, a: ContextParameterTypeA) val validIdentifier1: ReturnType get() = null!!


// member callables

class ReceiverType {
    context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONFLICTING_OVERLOADS!>fun conflictingMemberFunction1(): ReturnType<!> = null!!
    context(b: ContextParameterTypeB, a: ContextParameterTypeA) <!CONFLICTING_OVERLOADS!>fun conflictingMemberFunction1(): ReturnType<!> = null!!

    context(a: ContextParameterTypeA, b: ContextParameterTypeB) val <!REDECLARATION!>conflictingMemberProperty1<!>: ReturnType get() = null!!
    context(b: ContextParameterTypeB, a: ContextParameterTypeA) val <!REDECLARATION!>conflictingMemberProperty1<!>: ReturnType get() = null!!

    // member callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    context(b: ContextParameterTypeB, a: ContextParameterTypeA) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(a: ContextParameterTypeA, b: ContextParameterTypeB) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    context(b: ContextParameterTypeB, a: ContextParameterTypeA) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    // member callables (in different c-level sets)

    context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun validIdentifier1(): ReturnType = null!!
    context(b: ContextParameterTypeB, a: ContextParameterTypeA) val validIdentifier1: ReturnType get() = null!!
}


// extension callables

context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONFLICTING_OVERLOADS!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!
context(b: ContextParameterTypeB, a: ContextParameterTypeA) <!CONFLICTING_OVERLOADS!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!

context(a: ContextParameterTypeA, b: ContextParameterTypeB) val ReceiverType.<!REDECLARATION!>conflictingExtensionProperty1<!>: ReturnType get() = null!!
context(b: ContextParameterTypeB, a: ContextParameterTypeA) val ReceiverType.<!REDECLARATION!>conflictingExtensionProperty1<!>: ReturnType get() = null!!

// extension callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

context(b: ContextParameterTypeB, a: ContextParameterTypeA) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterTypeA, b: ContextParameterTypeB) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(b: ContextParameterTypeB, a: ContextParameterTypeA) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

// extension callables (in different c-level sets)

context(a: ContextParameterTypeA, b: ContextParameterTypeB) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1<!>(): ReturnType = null!!
context(b: ContextParameterTypeB, a: ContextParameterTypeA) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1<!>: ReturnType get() = null!!


// local callables

fun localScope() {
    context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONFLICTING_OVERLOADS!>fun conflictingLocalFunction1(): ReturnType<!> = null!!
    context(b: ContextParameterTypeB, a: ContextParameterTypeA) <!CONFLICTING_OVERLOADS!>fun conflictingLocalFunction1(): ReturnType<!> = null!!

    // local callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(a: ContextParameterTypeA, b: ContextParameterTypeB) <!CONFLICTING_OVERLOADS!>fun validLocalFunctionViaHidingDeprecation1(): ReturnType<!> = null!!

    context(b: ContextParameterTypeB, a: ContextParameterTypeA) <!CONFLICTING_OVERLOADS!>fun validLocalFunctionViaHidingDeprecation1(): ReturnType<!> = null!!
}


/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, interfaceDeclaration, localFunction, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral */
