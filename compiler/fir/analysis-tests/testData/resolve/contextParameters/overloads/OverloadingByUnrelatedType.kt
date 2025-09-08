// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// WITH_STDLIB


interface ContextParameterTypeA
interface ContextParameterTypeB
interface GenericContextParameterType<T>
interface TypeArgumentA
interface TypeArgumentB
interface TypeArgument
interface ContextParameterReturnType
interface ReturnType


// top-level callables

context(ctx: ContextParameterTypeA) fun validTopLevelFunction(): ReturnType = null!!
context(ctx: ContextParameterTypeB) fun validTopLevelFunction(): ReturnType = null!!
@JvmName("vtlfa") context(ctx: GenericContextParameterType<TypeArgumentA>) fun validTopLevelFunction(): ReturnType = null!!
@JvmName("vtlfb") context(ctx: GenericContextParameterType<TypeArgumentB>) fun validTopLevelFunction(): ReturnType = null!!
@JvmName("vtlfo") context(ctx: GenericContextParameterType<out TypeArgument>) fun validTopLevelFunction(): ReturnType = null!!
@JvmName("vtlfi") context(ctx: GenericContextParameterType<in TypeArgument>) fun validTopLevelFunction(): ReturnType = null!!
context(ctx: () -> ContextParameterReturnType) fun validTopLevelFunction(): ReturnType = null!!
context(ctx: suspend () -> ContextParameterReturnType) fun validTopLevelFunction(): ReturnType = null!!

context(ctx: ContextParameterTypeA) val validTopLevelProperty: ReturnType get() = null!!
context(ctx: ContextParameterTypeB) val validTopLevelProperty: ReturnType get() = null!!
context(ctx: GenericContextParameterType<TypeArgumentA>) val validTopLevelProperty: ReturnType @JvmName("vtlpa") get() = null!!
context(ctx: GenericContextParameterType<TypeArgumentB>) val validTopLevelProperty: ReturnType @JvmName("vtlpb") get() = null!!
context(ctx: GenericContextParameterType<out TypeArgument>) val validTopLevelProperty: ReturnType @JvmName("vtlpo") get() = null!!
context(ctx: GenericContextParameterType<in TypeArgument>) val validTopLevelProperty: ReturnType @JvmName("vtlpi") get() = null!!
context(ctx: () -> ContextParameterReturnType) val validTopLevelProperty: ReturnType get() = null!!
context(ctx: suspend () -> ContextParameterReturnType) val validTopLevelProperty: ReturnType get() = null!!


// member callables

class ReceiverType {
    context(ctx: ContextParameterTypeA) fun validMemberFunction(): ReturnType = null!!
    context(ctx: ContextParameterTypeB) fun validMemberFunction(): ReturnType = null!!
    @JvmName("vmfa") context(ctx: GenericContextParameterType<TypeArgumentA>) fun validMemberFunction(): ReturnType = null!!
    @JvmName("vmfb") context(ctx: GenericContextParameterType<TypeArgumentB>) fun validMemberFunction(): ReturnType = null!!
    @JvmName("vmfo") context(ctx: GenericContextParameterType<out TypeArgument>) fun validMemberFunction(): ReturnType = null!!
    @JvmName("vmfi") context(ctx: GenericContextParameterType<in TypeArgument>) fun validMemberFunction(): ReturnType = null!!
    context(ctx: () -> ContextParameterReturnType) fun validMemberFunction(): ReturnType = null!!
    context(ctx: suspend () -> ContextParameterReturnType) fun validMemberFunction(): ReturnType = null!!

    context(ctx: ContextParameterTypeA) val validMemberProperty: ReturnType get() = null!!
    context(ctx: ContextParameterTypeB) val validMemberProperty: ReturnType get() = null!!
    context(ctx: GenericContextParameterType<TypeArgumentA>) val validMemberProperty: ReturnType @JvmName("vmpa") get() = null!!
    context(ctx: GenericContextParameterType<TypeArgumentB>) val validMemberProperty: ReturnType @JvmName("vmpb") get() = null!!
    context(ctx: GenericContextParameterType<out TypeArgument>) val validMemberProperty: ReturnType @JvmName("vmpo") get() = null!!
    context(ctx: GenericContextParameterType<in TypeArgument>) val validMemberProperty: ReturnType @JvmName("vmpi") get() = null!!
    context(ctx: () -> ContextParameterReturnType) val validMemberProperty: ReturnType get() = null!!
    context(ctx: suspend () -> ContextParameterReturnType) val validMemberProperty: ReturnType get() = null!!
}


// extension callables

context(ctx: ContextParameterTypeA) fun ReceiverType.validExtensionFunction(): ReturnType = null!!
context(ctx: ContextParameterTypeB) fun ReceiverType.validExtensionFunction(): ReturnType = null!!
@JvmName("vefa") context(ctx: GenericContextParameterType<TypeArgumentA>) fun ReceiverType.validExtensionFunction(): ReturnType = null!!
@JvmName("vefb") context(ctx: GenericContextParameterType<TypeArgumentB>) fun ReceiverType.validExtensionFunction(): ReturnType = null!!
@JvmName("vefo") context(ctx: GenericContextParameterType<out TypeArgument>) fun ReceiverType.validExtensionFunction(): ReturnType = null!!
@JvmName("vefi") context(ctx: GenericContextParameterType<in TypeArgument>) fun ReceiverType.validExtensionFunction(): ReturnType = null!!
context(ctx: () -> ContextParameterReturnType) fun ReceiverType.validExtensionFunction(): ReturnType = null!!
context(ctx: suspend () -> ContextParameterReturnType) fun ReceiverType.validExtensionFunction(): ReturnType = null!!

context(ctx: ContextParameterTypeA) val ReceiverType.validExtensionProperty: ReturnType get() = null!!
context(ctx: ContextParameterTypeB) val ReceiverType.validExtensionProperty: ReturnType get() = null!!
context(ctx: GenericContextParameterType<TypeArgumentA>) val ReceiverType.validExtensionProperty: ReturnType @JvmName("vepa") get() = null!!
context(ctx: GenericContextParameterType<TypeArgumentB>) val ReceiverType.validExtensionProperty: ReturnType @JvmName("vepb") get() = null!!
context(ctx: GenericContextParameterType<out TypeArgument>) val ReceiverType.validExtensionProperty: ReturnType @JvmName("vepo") get() = null!!
context(ctx: GenericContextParameterType<in TypeArgument>) val ReceiverType.validExtensionProperty: ReturnType @JvmName("vepi") get() = null!!
context(ctx: () -> ContextParameterReturnType) val ReceiverType.validExtensionProperty: ReturnType get() = null!!
context(ctx: suspend () -> ContextParameterReturnType) val ReceiverType.validExtensionProperty: ReturnType get() = null!!


// local callables

fun localScope() {
    context(ctx: ContextParameterTypeA) fun validLocalFunction(): ReturnType = null!!
    context(ctx: ContextParameterTypeB) fun validLocalFunction(): ReturnType = null!!
    context(ctx: GenericContextParameterType<TypeArgumentA>) fun validLocalFunction(): ReturnType = null!!
    context(ctx: GenericContextParameterType<TypeArgumentB>) fun validLocalFunction(): ReturnType = null!!
    context(ctx: GenericContextParameterType<out TypeArgument>) fun validLocalFunction(): ReturnType = null!!
    context(ctx: GenericContextParameterType<in TypeArgument>) fun validLocalFunction(): ReturnType = null!!
    context(ctx: () -> ContextParameterReturnType) fun validLocalFunction(): ReturnType = null!!
    context(ctx: suspend () -> ContextParameterReturnType) fun validLocalFunction(): ReturnType = null!!
}


/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, functionalType, getter, inProjection, interfaceDeclaration, localFunction, nullableType,
outProjection, propertyDeclaration, propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral,
suspend, typeParameter */
