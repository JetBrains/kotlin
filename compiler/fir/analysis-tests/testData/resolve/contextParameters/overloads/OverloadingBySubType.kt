// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters


interface ContextParameterSuperType
interface ContextParameterSubType: ContextParameterSuperType
interface ContextParameterType
interface GenericContextParameterType<T>
interface SuperTypeArgument
interface SubTypeArgument: SuperTypeArgument
interface ReturnType


// top-level callables

context(ctx: ContextParameterSuperType) fun conflictingTopLevelFunction1(): ReturnType = null!!
context(ctx: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingTopLevelFunction1(): ReturnType<!> = null!!

context(ctx: ContextParameterType?) fun conflictingTopLevelFunction2(): ReturnType = null!!
context(ctx: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingTopLevelFunction2(): ReturnType<!> = null!!

context(ctx: GenericContextParameterType<out SuperTypeArgument>) fun conflictingTopLevelFunction3(): ReturnType = null!!
context(ctx: GenericContextParameterType<out SubTypeArgument>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingTopLevelFunction3(): ReturnType<!> = null!!

context(ctx: GenericContextParameterType<in SubTypeArgument>) fun conflictingTopLevelFunction4(): ReturnType = null!!
context(ctx: GenericContextParameterType<in SuperTypeArgument>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingTopLevelFunction4(): ReturnType<!> = null!!

context(ctx: GenericContextParameterType<out ContextParameterType>) fun conflictingTopLevelFunction5(): ReturnType = null!!
context(ctx: GenericContextParameterType<ContextParameterType>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingTopLevelFunction5(): ReturnType<!> = null!!

context(ctx: GenericContextParameterType<in ContextParameterType>) fun conflictingTopLevelFunction6(): ReturnType = null!!
context(ctx: GenericContextParameterType<ContextParameterType>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingTopLevelFunction6(): ReturnType<!> = null!!

context(ctx: CTLF7) <!CONFLICTING_OVERLOADS!>fun <CTLF7> conflictingTopLevelFunction7(): ReturnType<!> = null!!
context(ctx: CTLF7?) <!CONFLICTING_OVERLOADS!>fun <CTLF7: Any> conflictingTopLevelFunction7(): ReturnType<!> = null!!

context(ctx: CTLF8?) fun <CTLF8: Any> conflictingTopLevelFunction8(): ReturnType = null!!
context(ctx: CTLF8) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun <CTLF8: Any> conflictingTopLevelFunction8(): ReturnType<!> = null!!

context(ctx: CTLF9) fun <CTLF9> conflictingTopLevelFunction9(): ReturnType = null!!
context(ctx: CTLF9 & Any) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun <CTLF9> conflictingTopLevelFunction9(): ReturnType<!> = null!!

context(ctx: CTLF10 & Any) <!CONFLICTING_OVERLOADS!>fun <CTLF10> conflictingTopLevelFunction10(): ReturnType<!> = null!!
context(ctx: CTLF10) <!CONFLICTING_OVERLOADS!>fun <CTLF10: Any> conflictingTopLevelFunction10(): ReturnType<!> = null!!

context(ctx: CTLF11) fun <CTLF11: ContextParameterSuperType> conflictingTopLevelFunction11(): ReturnType = null!!
context(ctx: CTLF11) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun <CTLF11: ContextParameterSubType> conflictingTopLevelFunction11(): ReturnType<!> = null!!

context(ctx: ContextParameterSuperType) fun <CTLF12> conflictingTopLevelFunction12(): ReturnType = null!!
context(ctx: CTLF12) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun <CTLF12: ContextParameterSubType> conflictingTopLevelFunction12(): ReturnType<!> = null!!

context(ctx: CTLF13) fun <CTLF13: ContextParameterSuperType> conflictingTopLevelFunction13(): ReturnType = null!!
context(ctx: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun <CTLF13> conflictingTopLevelFunction13(): ReturnType<!> = null!!

context(ctx: ContextParameterType) <!CONFLICTING_OVERLOADS!>fun <CTLF14> conflictingTopLevelFunction14(): ReturnType<!> = null!!
context(ctx: CTLF14) <!CONFLICTING_OVERLOADS!>fun <CTLF14: ContextParameterType> conflictingTopLevelFunction14(): ReturnType<!> = null!!

context(ctx: ContextParameterSuperType) val conflictingTopLevelProperty1: ReturnType get() = null!!
context(ctx: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingTopLevelProperty1: ReturnType<!> get() = null!!

context(ctx: ContextParameterType?) val conflictingTopLevelProperty2: ReturnType get() = null!!
context(ctx: ContextParameterType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingTopLevelProperty2: ReturnType<!> get() = null!!

context(ctx: GenericContextParameterType<out SuperTypeArgument>) val conflictingTopLevelProperty3: ReturnType get() = null!!
context(ctx: GenericContextParameterType<out SubTypeArgument>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingTopLevelProperty3: ReturnType<!> get() = null!!

context(ctx: GenericContextParameterType<in SubTypeArgument>) val conflictingTopLevelProperty4: ReturnType get() = null!!
context(ctx: GenericContextParameterType<in SuperTypeArgument>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingTopLevelProperty4: ReturnType<!> get() = null!!

context(ctx: GenericContextParameterType<out ContextParameterType>) val conflictingTopLevelProperty5: ReturnType get() = null!!
context(ctx: GenericContextParameterType<ContextParameterType>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingTopLevelProperty5: ReturnType<!> get() = null!!

context(ctx: GenericContextParameterType<in ContextParameterType>) val conflictingTopLevelProperty6: ReturnType get() = null!!
context(ctx: GenericContextParameterType<ContextParameterType>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingTopLevelProperty6: ReturnType<!> get() = null!!

context(ctx: CTLP7) val <CTLP7> <!REDECLARATION!>conflictingTopLevelProperty7<!>: ReturnType get() = null!!
context(ctx: CTLP7?) val <CTLP7: Any> <!REDECLARATION!>conflictingTopLevelProperty7<!>: ReturnType get() = null!!

context(ctx: CTLP8?) val <CTLP8: Any> conflictingTopLevelProperty8: ReturnType get() = null!!
context(ctx: CTLP8) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <CTLP8: Any> conflictingTopLevelProperty8: ReturnType<!> get() = null!!

context(ctx: CTLP9) val <CTLP9> conflictingTopLevelProperty9: ReturnType get() = null!!
context(ctx: CTLP9 & Any) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <CTLP9> conflictingTopLevelProperty9: ReturnType<!> get() = null!!

context(ctx: CTLP10 & Any) val <CTLP10> <!REDECLARATION!>conflictingTopLevelProperty10<!>: ReturnType get() = null!!
context(ctx: CTLP10) val <CTLP10: Any> <!REDECLARATION!>conflictingTopLevelProperty10<!>: ReturnType get() = null!!

context(ctx: CTLP11) val <CTLP11: ContextParameterSuperType> conflictingTopLevelProperty11: ReturnType get() = null!!
context(ctx: CTLP11) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <CTLP11: ContextParameterSubType> conflictingTopLevelProperty11: ReturnType<!> get() = null!!

// top-level callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterSuperType) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

context(ctx: ContextParameterSubType) fun validTopLevelFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterSuperType) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(ctx: ContextParameterSubType) val validTopLevelPropertyViaHidingDeprecation1: ReturnType get() = null!!

// top-level callables (in different c-level sets)

context(ctx: ContextParameterSuperType) fun validIdentifier1a(): ReturnType = null!!
context(ctx: ContextParameterSubType) val validIdentifier1a: ReturnType get() = null!!

context(ctx: ContextParameterSubType) fun validIdentifier1b(): ReturnType = null!!
context(ctx: ContextParameterSuperType) val validIdentifier1b: ReturnType get() = null!!


// member callables

class ReceiverType {
    context(ctx: ContextParameterSuperType) fun conflictingMemberFunction1(): ReturnType = null!!
    context(ctx: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingMemberFunction1(): ReturnType<!> = null!!

    context(ctx: ContextParameterSuperType) val conflictingMemberProperty1: ReturnType get() = null!!
    context(ctx: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val conflictingMemberProperty1: ReturnType<!> get() = null!!

    // member callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterSuperType) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    context(ctx: ContextParameterSubType) fun validMemberFunctionViaHidingDeprecation1(): ReturnType = null!!

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterSuperType) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    context(ctx: ContextParameterSubType) val validMemberPropertyViaHidingDeprecation1: ReturnType get() = null!!

    // member callables (in different c-level sets)

    context(ctx: ContextParameterSuperType) fun validIdentifier1a(): ReturnType = null!!
    context(ctx: ContextParameterSubType) val validIdentifier1a: ReturnType get() = null!!

    context(ctx: ContextParameterSubType) fun validIdentifier1b(): ReturnType = null!!
    context(ctx: ContextParameterSuperType) val validIdentifier1b: ReturnType get() = null!!
}


// extension callables

context(ctx: ContextParameterSuperType) fun ReceiverType.conflictingExtensionFunction1(): ReturnType = null!!
context(ctx: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun ReceiverType.conflictingExtensionFunction1(): ReturnType<!> = null!!

context(ctx: ContextParameterSuperType) val ReceiverType.conflictingExtensionProperty1: ReturnType get() = null!!
context(ctx: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val ReceiverType.conflictingExtensionProperty1: ReturnType<!> get() = null!!

// extension callables (w/ hidden deprecated declarations)

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterSuperType) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

context(ctx: ContextParameterSubType) fun ReceiverType.validExtensionFunctionViaHidingDeprecation1(): ReturnType = null!!

@Deprecated("", level = DeprecationLevel.HIDDEN)
context(ctx: ContextParameterSuperType) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

context(ctx: ContextParameterSubType) val ReceiverType.validExtensionPropertyViaHidingDeprecation1: ReturnType get() = null!!

// extension callables (in different c-level sets)

context(ctx: ContextParameterSuperType) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1a<!>(): ReturnType = null!!
context(ctx: ContextParameterSubType) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1a<!>: ReturnType get() = null!!

context(ctx: ContextParameterSubType) fun ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1b<!>(): ReturnType = null!!
context(ctx: ContextParameterSuperType) val ReceiverType.<!EXTENSION_SHADOWED_BY_MEMBER!>validIdentifier1b<!>: ReturnType get() = null!!


// local callables

fun localScope() {
    context(ctx: ContextParameterSuperType) fun conflictingLocalFunction1(): ReturnType = null!!
    context(ctx: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun conflictingLocalFunction1(): ReturnType<!> = null!!

    // local callables (w/ hidden deprecated declarations)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    context(ctx: ContextParameterSuperType) fun validLocalFunctionViaHidingDeprecation1(): ReturnType = null!!

    context(ctx: ContextParameterSubType) <!CONTEXTUAL_OVERLOAD_SHADOWED!>fun validLocalFunctionViaHidingDeprecation1(): ReturnType<!> = null!!
}


/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, dnnType, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, inProjection, interfaceDeclaration, localFunction, nullableType, outProjection,
propertyDeclaration, propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral, typeConstraint,
typeParameter */
