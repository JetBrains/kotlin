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
        val allSupportedByAbiReader: List<AbiSignatureVersion> get() = AbiSignatureVersions.Supported.entries
        fun resolveByVersionNumber(versionNumber: Int): AbiSignatureVersion = AbiSignatureVersions.resolveByVersionNumber(versionNumber)
    }
}

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

    val nameSegments: List<AbiSimpleName> get() = value.split(SEPARATOR).map(::AbiSimpleName)
    val nameSegmentsCount: Int get() = value.count { it == SEPARATOR } + 1

    val simpleName: AbiSimpleName get() = AbiSimpleName(value.substringAfterLast(SEPARATOR))

    override fun compareTo(other: AbiCompoundName) = value.compareTo(other.value)
    override fun toString() = value

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
        const val SEPARATOR = '/'
    }
}

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

@ExperimentalLibraryAbiReader
sealed interface AbiDeclarationWithModality : AbiDeclaration {
    val modality: AbiModality
}

@ExperimentalLibraryAbiReader
enum class AbiModality {
    FINAL, OPEN, ABSTRACT, SEALED
}

@ExperimentalLibraryAbiReader
interface AbiDeclarationContainer {
    /** Important: The order of declarations is preserved exactly as in serialized IR. */
    val declarations: List<AbiDeclaration>
}

@ExperimentalLibraryAbiReader
interface AbiTopLevelDeclarations : AbiDeclarationContainer

@ExperimentalLibraryAbiReader
interface AbiClass : AbiDeclarationWithModality, AbiDeclarationContainer, AbiTypeParametersContainer {
    val kind: AbiClassKind
    val isInner: Boolean
    val isValue: Boolean
    val isFunction: Boolean

    /**
     * The set of non-trivial supertypes (i.e. excluding [kotlin.Any]).
     * Important: The order of supertypes is preserved exactly as in serialized IR.
     */
    val superTypes: List<AbiType>
}

@ExperimentalLibraryAbiReader
enum class AbiClassKind {
    CLASS, INTERFACE, OBJECT, ENUM_CLASS, ANNOTATION_CLASS
}

@ExperimentalLibraryAbiReader
interface AbiEnumEntry : AbiDeclaration

@ExperimentalLibraryAbiReader
interface AbiFunction : AbiDeclarationWithModality, AbiTypeParametersContainer {
    val isConstructor: Boolean
    val isInline: Boolean
    val isSuspend: Boolean
    val hasExtensionReceiverParameter: Boolean
    val contextReceiverParametersCount: Int

    /**
     * Important: All value parameters of the function are stored in the single place, in the [valueParameters] list in
     * a well-defined order. First, unless [hasExtensionReceiverParameter] is false, goes the extension receiver parameter.
     * It is followed by [contextReceiverParametersCount] context receiver parameters. The remainder are the regular
     * value parameters of the function.
     */
    val valueParameters: List<AbiValueParameter>
    val returnType: AbiType?
}

@ExperimentalLibraryAbiReader
interface AbiValueParameter {
    val type: AbiType
    val isVararg: Boolean
    val hasDefaultArg: Boolean
    val isNoinline: Boolean
    val isCrossinline: Boolean
}

@ExperimentalLibraryAbiReader
interface AbiProperty : AbiDeclarationWithModality {
    val kind: AbiPropertyKind
    val getter: AbiFunction?
    val setter: AbiFunction?
}

@ExperimentalLibraryAbiReader
enum class AbiPropertyKind { VAL, CONST_VAL, VAR }

@ExperimentalLibraryAbiReader
sealed interface AbiTypeParametersContainer : AbiDeclaration {
    /** Important: The order of [typeParameters] is preserved exactly as in serialized IR. */
    val typeParameters: List<AbiTypeParameter>
}

@ExperimentalLibraryAbiReader
interface AbiTypeParameter {
    val tag: String
    val variance: AbiVariance
    val isReified: Boolean

    /**
     * The set of non-trivial upper bounds (i.e. excluding nullable [kotlin.Any]).
     * Important: The order of upper bounds is preserved exactly as in serialized IR.
     */
    val upperBounds: List<AbiType>
}

@ExperimentalLibraryAbiReader
sealed interface AbiType {
    interface Dynamic : AbiType
    interface Error : AbiType
    interface Simple : AbiType {
        val classifierReference: AbiClassifierReference
        val arguments: List<AbiTypeArgument>
        val nullability: AbiTypeNullability
    }
}

@ExperimentalLibraryAbiReader
sealed interface AbiTypeArgument {
    interface StarProjection : AbiTypeArgument
    interface TypeProjection : AbiTypeArgument {
        val type: AbiType
        val variance: AbiVariance
    }
}

@ExperimentalLibraryAbiReader
sealed interface AbiClassifierReference {
    interface ClassReference : AbiClassifierReference {
        val className: AbiQualifiedName
    }

    interface TypeParameterReference : AbiClassifierReference {
        val tag: String
    }
}

@ExperimentalLibraryAbiReader
enum class AbiTypeNullability {
    MARKED_NULLABLE, NOT_SPECIFIED, DEFINITELY_NOT_NULL
}

@ExperimentalLibraryAbiReader
enum class AbiVariance {
    INVARIANT, IN, OUT
}
