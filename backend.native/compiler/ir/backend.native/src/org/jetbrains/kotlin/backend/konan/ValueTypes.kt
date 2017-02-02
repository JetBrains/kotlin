package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.KotlinType

/**
 * Value type is a kind of Kotlin types represented as plain value in generated code, not as an object reference.
 * Such types may require autoboxing.
 *
 * The value type nearly corresponds to `[classFqName]` or `[classFqName]?` Kotlin type (depending on [isNullable]).
 *
 * @property classFqName name of the base value type class
 * @property isNullable whether `null` is included into this value type
 */
enum class ValueType(val classFqName: FqNameUnsafe, val isNullable: Boolean = false) {
    BOOLEAN(KotlinBuiltIns.FQ_NAMES._boolean),
    CHAR(KotlinBuiltIns.FQ_NAMES._char),

    BYTE(KotlinBuiltIns.FQ_NAMES._byte),
    SHORT(KotlinBuiltIns.FQ_NAMES._short),
    INT(KotlinBuiltIns.FQ_NAMES._int),
    LONG(KotlinBuiltIns.FQ_NAMES._long),
    FLOAT(KotlinBuiltIns.FQ_NAMES._float),
    DOUBLE(KotlinBuiltIns.FQ_NAMES._double),

    UNBOUND_CALLABLE_REFERENCE(FqNameUnsafe("konan.internal.UnboundCallableReference"))
}

private fun KotlinType.isConstructedFromGivenClass(fqName: FqNameUnsafe) =
        KotlinBuiltIns.isConstructedFromGivenClass(this, fqName)

/**
 * @return `true` if this type must be represented as given value type in generated code.
 */
tailrec fun KotlinType.isRepresentedAs(valueType: ValueType): Boolean {
    if (this.isMarkedNullable && !valueType.isNullable) {
        return false
    }

    if (this.isConstructedFromGivenClass(valueType.classFqName)) {
        return true
    }

    // Supertypes should be checked even for "final" value types (e.g. Int)
    // to treat type parameter `T` with `Int` upper bound as value type.
    // This behavior is observed on Kotlin JVM and used in interop implementation.
    //
    // However to optimize this method only first supertype is checked
    // (it is supposed to be enough in all sane cases).
    val firstSupertype = this.constructor.supertypes.firstOrNull() ?: return false
    return firstSupertype.isRepresentedAs(valueType)
}

/**
 * @return `true` if this type without `null` value must be represented as given value type in generated code.
 *
 * TODO: this method can be considered as a hack; rework its usages.
 */
tailrec fun KotlinType.notNullableIsRepresentedAs(valueType: ValueType): Boolean {
    if (this.isConstructedFromGivenClass(valueType.classFqName)) {
        return true
    }

    // See comment in [isRepresentedAs].
    val firstSupertype = this.constructor.supertypes.firstOrNull() ?: return false
    return firstSupertype.notNullableIsRepresentedAs(valueType)
}