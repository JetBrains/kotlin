// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters


interface ContextParameterType
interface ReturnType


// top-level callables

context(a: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!
context(b: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!

context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction2(): ReturnType<!> = null!!
context(_: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction2(): ReturnType<!> = null!!

context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction3(): ReturnType<!> = null!!
context(`_`: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction3(): ReturnType<!> = null!!

context(_: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction4(): ReturnType<!> = null!!
context(`_`: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction4(): ReturnType<!> = null!!

context(a: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty1<!>: ReturnType get() = null!!
context(b: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty1<!>: ReturnType get() = null!!

context(ctx: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty2<!>: ReturnType get() = null!!
context(_: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty2<!>: ReturnType get() = null!!

context(ctx: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty3<!>: ReturnType get() = null!!
context(`_`: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty3<!>: ReturnType get() = null!!

context(_: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty4<!>: ReturnType get() = null!!
context(`_`: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty4<!>: ReturnType get() = null!!

// top-level callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterType) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

context(b: ContextParameterType) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterType) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(b: ContextParameterType) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

// top-level callables (in different c-level sets)

context(a: ContextParameterType) fun validIdentifier1(): ReturnType = null!!
context(b: ContextParameterType) val validIdentifier1: ReturnType get() = null!!


// member callables

class ReceiverType {
    context(a: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingMemberFunction1(): ReturnType<!> = null!!
    context(b: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingMemberFunction1(): ReturnType<!> = null!!

    context(a: ContextParameterType) val <!REDECLARATION!>conflictingMemberProperty1<!>: ReturnType get() = null!!
    context(b: ContextParameterType) val <!REDECLARATION!>conflictingMemberProperty1<!>: ReturnType get() = null!!

    // member callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(a: ContextParameterType) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    context(b: ContextParameterType) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(a: ContextParameterType) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    context(b: ContextParameterType) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    // member callables (in different c-level sets)

    context(a: ContextParameterType) fun validIdentifier1(): ReturnType = null!!
    context(b: ContextParameterType) val validIdentifier1: ReturnType get() = null!!
}


// extension callables

context(a: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!
context(b: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!

context(a: ContextParameterType) val ReceiverType.<!REDECLARATION!>conflictingExtensionProperty1<!>: ReturnType get() = null!!
context(b: ContextParameterType) val ReceiverType.<!REDECLARATION!>conflictingExtensionProperty1<!>: ReturnType get() = null!!

// extension callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterType) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

context(b: ContextParameterType) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(a: ContextParameterType) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(b: ContextParameterType) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

// extension callables (in different c-level sets)

context(a: ContextParameterType) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1<!>(): ReturnType = null!!
context(b: ContextParameterType) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1<!>: ReturnType get() = null!!


// local callables

fun localScope() {
    context(a: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingLocalFunction1(): ReturnType<!> = null!!
    context(b: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingLocalFunction1(): ReturnType<!> = null!!

    // local callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(a: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun validLocalFunctionViaHidingDeprecation1(): ReturnType<!> = null!!

    context(b: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun validLocalFunctionViaHidingDeprecation1(): ReturnType<!> = null!!
}


/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, interfaceDeclaration, localFunction, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral */
