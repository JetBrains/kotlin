// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters


interface ContextParameterType
typealias ContextParameterTypeAlias = ContextParameterType
interface ReturnType


// top-level callables

context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!
context(ctx: ContextParameterTypeAlias) <!CONFLICTING_OVERLOADS!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!

context(ctx: ContextParameterType) val <!REDECLARATION!>conflictingTopLevelProperty1<!>: ReturnType get() = null!!
context(ctx: ContextParameterTypeAlias) val <!REDECLARATION!>conflictingTopLevelProperty1<!>: ReturnType get() = null!!

// top-level callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterType) fun validTopLevelFunctionViaHidingDeprecation1a(): ReturnType = null!!

context(ctx: ContextParameterTypeAlias) fun validTopLevelFunctionViaHidingDeprecation1a(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterTypeAlias) fun validTopLevelFunctionViaHidingDeprecation1b(): ReturnType = null!!

context(ctx: ContextParameterType) fun validTopLevelFunctionViaHidingDeprecation1b(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterType) val validTopLevelPropertyViaHidingDeprecation1a: ReturnType get() = null!!

context(ctx: ContextParameterTypeAlias) val validTopLevelPropertyViaHidingDeprecation1a: ReturnType get() = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterTypeAlias) val validTopLevelPropertyViaHidingDeprecation1b: ReturnType get() = null!!

context(ctx: ContextParameterType) val validTopLevelPropertyViaHidingDeprecation1b: ReturnType get() = null!!

// top-level callables (in different c-level sets)

context(ctx: ContextParameterType) fun validIdentifier1a(): ReturnType = null!!
context(ctx: ContextParameterTypeAlias) val validIdentifier1a: ReturnType get() = null!!

context(ctx: ContextParameterTypeAlias) fun validIdentifier1b(): ReturnType = null!!
context(ctx: ContextParameterType) val validIdentifier1b: ReturnType get() = null!!


// member callables

class ReceiverType {
    context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingMemberFunction1(): ReturnType<!> = null!!
    context(ctx: ContextParameterTypeAlias) <!CONFLICTING_OVERLOADS!>fun conflictingMemberFunction1(): ReturnType<!> = null!!

    context(ctx: ContextParameterType) val <!REDECLARATION!>conflictingMemberProperty1<!>: ReturnType get() = null!!
    context(ctx: ContextParameterTypeAlias) val <!REDECLARATION!>conflictingMemberProperty1<!>: ReturnType get() = null!!

    // member callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterType) fun validMemberFunctionViaHidingDeprecation1a(): ReturnType = null!!

    context(ctx: ContextParameterTypeAlias) fun validMemberFunctionViaHidingDeprecation1a(): ReturnType = null!!

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterTypeAlias) fun validMemberFunctionViaHidingDeprecation1b(): ReturnType = null!!

    context(ctx: ContextParameterType) fun validMemberFunctionViaHidingDeprecation1b(): ReturnType = null!!

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterType) val validMemberPropertyViaHidingDeprecation1a: ReturnType get() = null!!

    context(ctx: ContextParameterTypeAlias) val validMemberPropertyViaHidingDeprecation1a: ReturnType get() = null!!

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterTypeAlias) val validMemberPropertyViaHidingDeprecation1b: ReturnType get() = null!!

    context(ctx: ContextParameterType) val validMemberPropertyViaHidingDeprecation1b: ReturnType get() = null!!

    // member callables (in different c-level sets)

    context(ctx: ContextParameterType) fun validIdentifier1a(): ReturnType = null!!
    context(ctx: ContextParameterTypeAlias) val validIdentifier1a: ReturnType get() = null!!

    context(ctx: ContextParameterTypeAlias) fun validIdentifier1b(): ReturnType = null!!
    context(ctx: ContextParameterType) val validIdentifier1b: ReturnType get() = null!!
}


// extension callables

context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!
context(ctx: ContextParameterTypeAlias) <!CONFLICTING_OVERLOADS!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!

context(ctx: ContextParameterType) val ReceiverType.<!REDECLARATION!>conflictingExtensionProperty1<!>: ReturnType get() = null!!
context(ctx: ContextParameterTypeAlias) val ReceiverType.<!REDECLARATION!>conflictingExtensionProperty1<!>: ReturnType get() = null!!

// extension callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterType) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1a(): ReturnType = null!!

context(ctx: ContextParameterTypeAlias) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1a(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterTypeAlias) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1b(): ReturnType = null!!

context(ctx: ContextParameterType) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1b(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterType) val ReceiverType.validExtensionPropertyViaHidingDeprecation1a: ReturnType get() = null!!

context(ctx: ContextParameterTypeAlias) val ReceiverType.validExtensionPropertyViaHidingDeprecation1a: ReturnType get() = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterTypeAlias) val ReceiverType.validExtensionPropertyViaHidingDeprecation1b: ReturnType get() = null!!

context(ctx: ContextParameterType) val ReceiverType.validExtensionPropertyViaHidingDeprecation1b: ReturnType get() = null!!

// extension callables (in different c-level sets)

context(ctx: ContextParameterType) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1a<!>(): ReturnType = null!!
context(ctx: ContextParameterTypeAlias) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1a<!>: ReturnType get() = null!!

context(ctx: ContextParameterTypeAlias) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1b<!>(): ReturnType = null!!
context(ctx: ContextParameterType) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1b<!>: ReturnType get() = null!!


// local callables

fun localScope() {
    context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun conflictingLocalFunction1(): ReturnType<!> = null!!
    context(ctx: ContextParameterTypeAlias) <!CONFLICTING_OVERLOADS!>fun conflictingLocalFunction1(): ReturnType<!> = null!!

    // local callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun validLocalFunctionViaHidingDeprecation1a(): ReturnType<!> = null!!

    context(ctx: ContextParameterTypeAlias) <!CONFLICTING_OVERLOADS!>fun validLocalFunctionViaHidingDeprecation1a(): ReturnType<!> = null!!

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterTypeAlias) <!CONFLICTING_OVERLOADS!>fun validLocalFunctionViaHidingDeprecation1b(): ReturnType<!> = null!!

    context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun validLocalFunctionViaHidingDeprecation1b(): ReturnType<!> = null!!
}


/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, interfaceDeclaration, localFunction, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral, typeAliasDeclaration */
