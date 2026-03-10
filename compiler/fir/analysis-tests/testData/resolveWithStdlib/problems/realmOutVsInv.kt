// RUN_PIPELINE_TILL: FRONTEND
// WITH_REFLECT
// DIAGNOSTICS: -UNCHECKED_CAST

import kotlin.reflect.KClass

interface Mediator
interface RealmReference
interface RealmObject : BaseRealmObject
interface NativePointer<T>
interface RealmSetT
typealias RealmSetPointer = NativePointer<RealmSetT>
interface RealmAny
interface BaseRealmObject

private fun <R> createSetOperator(
    setPtr: RealmSetPointer,
    clazz: KClass<R & Any>,
    mediator: Mediator,
    realm: RealmReference,
    operatorType: CollectionOperatorType,
): SetOperator<R> {
    return <!RETURN_TYPE_MISMATCH("SetOperator<R (of fun <R> createSetOperator)>; SetOperator<out R (of fun <R> createSetOperator)>")!>when (operatorType) {
        CollectionOperatorType.PRIMITIVE -> PrimitiveSetOperator(
            mediator,
            realm,
            converter(clazz),
            setPtr
        )
        CollectionOperatorType.REALM_ANY -> RealmAnySetOperator(
            mediator,
            realm,
            setPtr,
        ) as SetOperator<R>
        CollectionOperatorType.REALM_OBJECT -> {
            RealmObjectSetOperator(
                mediator,
                realm,
                setPtr,
                clazz as KClass<RealmObject>,
            ) as SetOperator<R>
        }
        else -> error("")
    }<!>
}

internal enum class CollectionOperatorType {
    PRIMITIVE,
    REALM_ANY,
    REALM_OBJECT,
    EMBEDDED_OBJECT
}

internal class PrimitiveSetOperator<E>(
    val mediator: Mediator,
    val realmReference: RealmReference,
    val realmValueConverter: RealmValueConverter<E>,
    val nativePointer: RealmSetPointer
) : SetOperator<E>

internal interface SetOperator<E> : CollectionOperator<E, RealmSetPointer>

internal interface CollectionOperator<E, T>

internal class RealmAnySetOperator(
    val mediator: Mediator,
    val realmReference: RealmReference,
    val nativePointer: RealmSetPointer,
) : SetOperator<RealmAny?>

internal class RealmObjectSetOperator<E : BaseRealmObject?>(
    mediator: Mediator,
    realmReference: RealmReference,
    nativePointer: RealmSetPointer,
    clazz: KClass<E & Any>
) : SetOperator<E>

internal fun <T> converter(clazz: KClass<T & Any>): RealmValueConverter<T> = error("")

internal interface RealmValueConverter<T>

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, dnnType, enumDeclaration, enumEntry, equalityExpression,
functionDeclaration, interfaceDeclaration, nullableType, override, primaryConstructor, propertyDeclaration, smartcast,
typeAliasDeclaration, typeConstraint, typeParameter, whenExpression, whenWithSubject */
