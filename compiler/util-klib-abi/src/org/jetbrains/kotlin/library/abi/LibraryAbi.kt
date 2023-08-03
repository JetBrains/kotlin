/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.abi.impl.AbiSignatureVersions

/**
 * The result of reading ABI from KLIB.
 *
 * @property manifest Information from the manifest that may be useful.
 * @property uniqueName The library's unique name that is a part of the library ABI.
 *   Corresponds to the `unique_name` manifest property.
 * @property signatureVersions The versions of signatures supported by the KLIB. Note that not every [AbiSignatureVersion]
 *   which is supported by the KLIB is also supported by the ABI reader. To check this please use
 *   [AbiSignatureVersion.isSupportedByAbiReader]. An attempt to obtain a signature of unsupported version will result
 *   in an exception. See also [AbiSignatures.get].
 * @property topLevelDeclarations The list of top-level declarations.
 */
@ExperimentalLibraryAbiReader
class LibraryAbi(
    val manifest: LibraryManifest,
    val uniqueName: String,
    val signatureVersions: Set<AbiSignatureVersion>,
    val topLevelDeclarations: AbiTopLevelDeclarations
)

/**
 * The representation of the version of IR signatures supported by a KLIB.
 *
 * @property versionNumber The unique version number of the IR signature.
 * @property isSupportedByAbiReader Whether this IR signature version is supported by the current implementation of
 *   the ABI reader. If it's not supported then such signatures can't be read by the ABI reader, and the version itself
 *   just serves for information purposes.
 * @property description Brief description of the IR signature version, if available. To be used purely for discovery
 *   purposes by ABI reader clients. Warning: The description text may be freely changed in the future. So, it should
 *   NEVER be used for making ABI snapshots.
 */
@ExperimentalLibraryAbiReader
interface AbiSignatureVersion {
    val versionNumber: Int
    val isSupportedByAbiReader: Boolean
    val description: String?

    companion object {
        /** All [AbiSignatureVersion]s supported by the current implementation of the ABI reader. */
        val allSupportedByAbiReader: List<AbiSignatureVersion> get() = AbiSignatureVersions.Supported.entries

        /**
         * A function to get an instance of [AbiSignatureVersion] by the unique [versionNumber].
         */
        fun resolveByVersionNumber(versionNumber: Int): AbiSignatureVersion = AbiSignatureVersions.resolveByVersionNumber(versionNumber)
    }
}

/**
 * A set of ABI signatures for a specific [AbiDeclaration]. This set can contain at most one signature per an [AbiSignatureVersion].
 */
@ExperimentalLibraryAbiReader
interface AbiSignatures {
    /**
     * Returns the signature of the specified [AbiSignatureVersion].
     *
     * - If the signature version is not supported by the ABI reader (according to [AbiSignatureVersion.isSupportedByAbiReader])
     *   then throw an exception.
     * - If the signature version is supported by the ABI reader, but the signature is unavailable for some other reason
     *   (e.g. a particular type of declaration misses a signature of a particular version), then return `null`.
     **/
    operator fun get(signatureVersion: AbiSignatureVersion): String?
}

/**
 * Simple name.
 * Examples: "TopLevelClass", "topLevelFun", "List", "EMPTY".
 */
@ExperimentalLibraryAbiReader
@JvmInline
value class AbiSimpleName(val value: String) : Comparable<AbiSimpleName> {
    init {
        require(AbiCompoundName.SEPARATOR !in value && AbiQualifiedName.SEPARATOR !in value) {
            "Simple name contains illegal characters: $value"
        }
    }

    override fun compareTo(other: AbiSimpleName) = value.compareTo(other.value)
    override fun toString() = value
}

/**
 * Compound name. An equivalent of one or more [AbiSimpleName]s which are concatenated with dots.
 * Examples: "TopLevelClass", "topLevelFun", "List", "CharRange.Companion.EMPTY".
 */
@ExperimentalLibraryAbiReader
@JvmInline
value class AbiCompoundName(val value: String) : Comparable<AbiCompoundName> {
    init {
        require(AbiQualifiedName.SEPARATOR !in value) { "Compound name contains illegal characters: $value" }
    }

    /** All name segments comprising this compound name. */
    val nameSegments: List<AbiSimpleName> get() = value.split(SEPARATOR).map(::AbiSimpleName)

    /** The count of name segments comprising this compound name. */
    val nameSegmentsCount: Int get() = value.count { it == SEPARATOR } + 1

    /** The right-most name segment of this compound name. */
    val simpleName: AbiSimpleName get() = AbiSimpleName(value.substringAfterLast(SEPARATOR))

    override fun compareTo(other: AbiCompoundName) = value.compareTo(other.value)
    override fun toString() = value

    /**
     * Whether a declaration with `this` instance of [AbiCompoundName] is a container of a declaration with `member` [AbiCompoundName].
     *
     * Examples:
     * ```
     * AbiCompoundName("") isContainerOf AbiCompoundName(<any>) == true
     * AbiCompoundName("foo.bar") isContainerOf AbiCompoundName("foo.bar.baz.qux") == true
     * AbiCompoundName("foo.bar") isContainerOf AbiCompoundName("foo.bar.baz") == true
     * AbiCompoundName("foo.bar") isContainerOf AbiCompoundName("foo.barbaz") == false
     * AbiCompoundName("foo.bar") isContainerOf AbiCompoundName("foo.bar") == false
     * AbiCompoundName("foo.bar") isContainerOf AbiCompoundName("foo") == false
     * ```
     */
    infix fun isContainerOf(member: AbiCompoundName): Boolean {
        val containerName = value
        return when (val containerNameLength = containerName.length) {
            0 -> true
            else -> {
                val memberName = member.value
                val memberNameLength = memberName.length
                memberNameLength > containerNameLength + 1 &&
                        memberName.startsWith(containerName) &&
                        memberName[containerNameLength] == SEPARATOR
            }
        }
    }

    companion object {
        /** The symbol that is used for separation of individual name segments in [AbiCompoundName]. */
        const val SEPARATOR = '.'
    }
}

/**
 * Fully qualified name.
 * Examples: "/TopLevelClass", "/topLevelFun", "kotlin.collections/List", "kotlin.ranges/CharRange.Companion.EMPTY".
 */
@ExperimentalLibraryAbiReader
data class AbiQualifiedName(val packageName: AbiCompoundName, val relativeName: AbiCompoundName) : Comparable<AbiQualifiedName> {
    init {
        require(relativeName.value.isNotEmpty()) { "Empty relative name" }
    }

    override fun compareTo(other: AbiQualifiedName): Int {
        val diff = packageName.compareTo(other.packageName)
        return if (diff != 0) diff else relativeName.compareTo(other.relativeName)
    }

    override fun toString() = "$packageName$SEPARATOR$relativeName"

    companion object {
        /**
         * The symbol that is used to separate the package name from the relative declaration name in
         * the single-string representation of [AbiQualifiedName].
         */
        const val SEPARATOR = '/'
    }
}

/**
 * The common interface for all declarations.
 *
 * @property qualifiedName The declaration qualified name.
 * @property signatures The set of signatures of the declaration.
 */
@ExperimentalLibraryAbiReader
sealed interface AbiDeclaration {
    val qualifiedName: AbiQualifiedName
    val signatures: AbiSignatures

    /**
     * Annotations are not a part of ABI. But sometimes it is useful to have the ability to check if some declaration
     * has a specific annotation. See [AbiReadingFilter.NonPublicMarkerAnnotations] as an example.
     */
    fun hasAnnotation(annotationClassName: AbiQualifiedName): Boolean
}

/**
 * A declaration that also has a modality.
 *
 * @property modality The modality of the declaration.
 */
@ExperimentalLibraryAbiReader
sealed interface AbiDeclarationWithModality : AbiDeclaration {
    val modality: AbiModality
}

/**
 * All known types of modality.
 */
@ExperimentalLibraryAbiReader
enum class AbiModality {
    FINAL, OPEN, ABSTRACT, SEALED
}

/**
 * The entity that can contain nested [AbiDeclaration]s.
 *
 * @property declarations Nested declarations.
 *   Important: The order of declarations is preserved exactly as in serialized IR.
 */
@ExperimentalLibraryAbiReader
interface AbiDeclarationContainer {
    val declarations: List<AbiDeclaration>
}

/**
 * The auxiliary container used just to keep together all top-level [AbiDeclaration]s in a KLIB.
 */
@ExperimentalLibraryAbiReader
interface AbiTopLevelDeclarations : AbiDeclarationContainer

/**
 * An [AbiDeclaration] that represents a Kotlin class.
 *
 * @property kind Class kind.
 * @property isInner Whether the class is an inner class.
 * @property isValue Whether the class is a value-class.
 * @property isFunction Whether the interface represented by this [AbiClass] is a fun-interface.
 * @property superTypes The set of non-trivial supertypes (i.e. excluding [kotlin.Any]).
 *   Important: The order of supertypes is preserved exactly as in serialized IR.
 */
@ExperimentalLibraryAbiReader
interface AbiClass : AbiDeclarationWithModality, AbiDeclarationContainer, AbiTypeParametersContainer {
    val kind: AbiClassKind
    val isInner: Boolean
    val isValue: Boolean
    val isFunction: Boolean
    val superTypes: List<AbiType>
}

/**
 * All known Kotlin class kinds.
 */
@ExperimentalLibraryAbiReader
enum class AbiClassKind {
    CLASS, INTERFACE, OBJECT, ENUM_CLASS, ANNOTATION_CLASS
}

/** An [AbiDeclaration] that represents a particular enum entry. */
@ExperimentalLibraryAbiReader
interface AbiEnumEntry : AbiDeclaration

/**
 * An [AbiDeclaration] that represents a function or a class constructor.
 *
 * @property isConstructor Whether this is a class constructor.
 * @property isInline Whether this is an `inline` function.
 * @property isSuspend Whether this is a `suspend` function.
 * @property hasExtensionReceiverParameter If this function has an extension receiver parameter.
 * @property contextReceiverParametersCount The number of context receiver parameters.
 * @property valueParameters The function value parameters.
 *   Important: All value parameters of the function are stored in the single place, in the [valueParameters] list in
 *   a well-defined order. First, unless [hasExtensionReceiverParameter] is false, goes the extension receiver parameter.
 *   It is followed by [contextReceiverParametersCount] context receiver parameters. The remainder are the regular
 *   value parameters of the function.
 * @property returnType The function's return type. Always `null` for constructors.
 */
@ExperimentalLibraryAbiReader
interface AbiFunction : AbiDeclarationWithModality, AbiTypeParametersContainer {
    val isConstructor: Boolean
    val isInline: Boolean
    val isSuspend: Boolean
    val hasExtensionReceiverParameter: Boolean
    val contextReceiverParametersCount: Int
    val valueParameters: List<AbiValueParameter>
    val returnType: AbiType?
}

/**
 * An individual value parameter of a function.
 *
 * @property type The type of the value parameter.
 * @property isVararg Whether the value parameter is a var-arg parameter.
 * @property hasDefaultArg Whether the value parameter has a default value.
 * @property isNoinline If the parameter is marked with `noinline` keyword.
 * @property isCrossinline If the parameter is marked with `crossinline` keyword.
 */
@ExperimentalLibraryAbiReader
interface AbiValueParameter {
    val type: AbiType
    val isVararg: Boolean
    val hasDefaultArg: Boolean
    val isNoinline: Boolean
    val isCrossinline: Boolean
}

/**
 * An [AbiDeclaration] that represents a property.
 *
 * @property kind The property kind.
 * @property getter The getter accessor, fi any.
 * @property setter The setter accessor, fi any.
 */
@ExperimentalLibraryAbiReader
interface AbiProperty : AbiDeclarationWithModality {
    val kind: AbiPropertyKind
    val getter: AbiFunction?
    val setter: AbiFunction?
}

/** All known kinds of properties. */
@ExperimentalLibraryAbiReader
enum class AbiPropertyKind { VAL, CONST_VAL, VAR }

/**
 * A declaration that also may have type parameters.
 *
 * @property typeParameters The declaration's type parameters.
 *   Important: The order of [typeParameters] is preserved exactly as in serialized IR.
 */
@ExperimentalLibraryAbiReader
sealed interface AbiTypeParametersContainer : AbiDeclaration {
    val typeParameters: List<AbiTypeParameter>
}

/**
 * An individual type parameter.
 *
 * @property tag A unique string identifying the type parameter withing the containing declaration's scope. Can be used
 *   to distinguish this type parameter from other type parameters. Important: Please note that the [tag] property is
 *   automatically generated by the ABI reader. The [tag] is not read from KLIB, and therefore it has no connection
 *   with the type parameter's name.
 * @property variance The type parameter variance.
 * @property isReified Whether the type parameter is a reified parameter.
 * @property upperBounds The set of non-trivial upper bounds (i.e. excluding nullable [kotlin.Any]).
 *   Important: The order of upper bounds is preserved exactly as in serialized IR.
 */
@ExperimentalLibraryAbiReader
interface AbiTypeParameter {
    val tag: String
    val variance: AbiVariance
    val isReified: Boolean
    val upperBounds: List<AbiType>
}

/** Represents a Kotlin type. Can be either of: [Dynamic], [Error], [Simple]. */
@ExperimentalLibraryAbiReader
sealed interface AbiType {
    /** The Kotlin/JavaScript `dynamic` type. */
    interface Dynamic : AbiType

    /**
     * The error type. Normally, this type should not occur in a KLIB, because by its own nature this type implies that
     * there was an error during compilation such that the compiler failed to fully resolve the type leading to compilation halted
     * even before anything has been written to the KLIB.
     */
    interface Error : AbiType

    /**
     * A regular type.
     *
     * @property classifierReference A reference pointing to the concrete classified used in the type.
     * @property arguments Type arguments.
     * @property nullability The nullability flag of the type.
     */
    interface Simple : AbiType {
        val classifierReference: AbiClassifierReference
        val arguments: List<AbiTypeArgument>
        val nullability: AbiTypeNullability
    }
}

/**
 * An individual argument of a type.
 */
@ExperimentalLibraryAbiReader
sealed interface AbiTypeArgument {
    /** A `<*>` (start projection) type argument. */
    interface StarProjection : AbiTypeArgument

    /**
     * A regular type argument.
     *
     * @property type The type argument's type.
     * @property variance The type arguemnt's variance.
     */
    interface TypeProjection : AbiTypeArgument {
        val type: AbiType
        val variance: AbiVariance
    }
}

/**
 * A reference to a concrete classifier. Used in [AbiType.Simple].
 */
@ExperimentalLibraryAbiReader
sealed interface AbiClassifierReference {
    /**
     * A reference that points to the concrete class.
     */
    interface ClassReference : AbiClassifierReference {
        val className: AbiQualifiedName
    }

    /**
     * A reference that points to a type parameter (the matching is done by [tag]).
     */
    interface TypeParameterReference : AbiClassifierReference {
        val tag: String
    }
}

/**
 * All known types of type nullability.
 */
@ExperimentalLibraryAbiReader
enum class AbiTypeNullability {
    MARKED_NULLABLE, NOT_SPECIFIED, DEFINITELY_NOT_NULL
}

/**
 * All known sorts of type argument's variance,
 */
@ExperimentalLibraryAbiReader
enum class AbiVariance {
    INVARIANT, IN, OUT
}
