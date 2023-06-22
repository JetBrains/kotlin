/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.ir.util.render as newRender

/**
 * [IdSignature] is a unique key that corresponds to each Kotlin Declaration. It is used to reference declarations in klib.
 * It has to be stable and unique. It’s a bit similar to JVM signature but more Kotlin-specific.
 *
 * In general, distinguishable declarations in Kotlin have to have different signatures.
 *
 * Some way to look at klib could be a map of [IdSignature] as a key and [org.jetbrains.kotlin.ir.declarations.IrDeclaration] as a value.
 *
 * There is a bunch of existing signatures which are used to reference different kinds of declaration.
 * Some of them may look weird because of some Kotlin specifics on one hand, and on the other hand, klib structure now is something similar
 * to a russian doll, where an inner declaration is nested inside its parent (a member inside a class), so we need a way to figure out
 * from a signature what the signature of its parent is. Moving to a flat structure could simplify signatures.
 *
 * One important definition needed is a *publicly-accessible declaration*, which means that there is a unique reference to a declaration `D`
 * outside the file in which the declaration `D` is declared, and the declaration `D` could be navigated across different files
 * (and modules) using that reference.
 *
 * ## Signature computation
 * First, let's consider a top-level non-private class `C` in a package `foo.bar`:
 * ```kotlin
 * package foo.bar
 * public class C<TP1, TP2> {
 *   fun qux() { ... }
 *   val p
 * }
 * ```
 *
 * The signatures of the class `C` and its type parameters `TPn` are going to be:
 * ```
 * CommonSignature {
 *   packageFqn = 'foo.bar'
 *   classFqn = 'C'
 *   hashCode = null
 *   flags = 0
 * }
 *
 * CompositeSignature { // TP1
 *    container = CommonSignature {
 *      packageFqn = 'foo.bar'
 *      classFqn = 'C'
 *      hashCode = null
 *     flags = 0
 *   }
 *   inner = LocalSignature {
 *      localFqn = '<TP>'
 *      hashCode = 0
 *   }
 * }
 *
 * CompositeSignature { // TP2
 *    container = CommonSignature {
 *      packageFqn = 'foo.bar'
 *      classFqn = 'C'
 *      hashCode = null
 *     flags = 0
 *   }
 *   inner = LocalSignature {
 *      localFqn = '<TP>'
 *      hashCode = 1
 *   }
 * }
 * ```
 *
 * The signature of the method `qux()` is:
 * ```
 * CommonSignature {
 *   packageFqn = 'foo.bar'
 *   classFqn = 'C.qux'
 *   hashCode = 0x..... // some hash code corresponding to the qux's signature
 *   flags = 0
 * }
 * ```
 *
 * The signature of the property `p` and the get-accessor of `p` are:
 * ```
 * CommonSignature {
 *   packageFqn = 'foo.bar'
 *   classFqn = 'C.p'
 *   hashCode = 0x..... // some hash code corresponding to the p's signature
 *   flags = 0
 * }
 *
 * AccessorSignature {
 *    propertySignature = CommonSignature {
 *     packageFqn = 'foo.bar'
 *     classFqn = 'C.p'
 *     hashCode = 0x..... // some hash code corresponding to the p's signature
 *     flags = 0
 *   }
 *   accessorSignature = CommonSignature {
 *     packageFqn = 'foo.bar'
 *     classFqn = 'C.p.<get-p>'
 *     hashCode = 0x..... // some hash code corresponding to the p.get's signature
 *     flags = 0
 *   }
 * }
 *
 * CompositeSignature {
 *   container = CommonSignature {
 *      packageFqn = 'foo.bar'
 *     classFqn = 'C.p'
 *      hashCode = 0x..... // some hash code corresponding to the p's signature
 *     flags = 0
 *   }
 *   inner = LocalSignature {
 *     localFqn = '<BF>'
 *     hashCode = null
 *   }
 * }
 * ```
 *
 * So, most of the cases are covered with [CommonSignature] and [AccessorSignature].
 * Now let take a look into less trivial cases.
 *
 * ```kotlin
 * // f1.kt
 * package foo.bar
 *
 * private class P {
 *
 * }
 *
 * private fun qux() { ... }
 *
 * // f2.kt
 * package foo.bar
 *
 * private fun qux() { ... }
 *
 * private class P {
 *
 * }
 * ```
 *
 * Here the signatures of `P` and `qux()` in `f1.kt` are going to be:
 * ```
 * CompositeSignature {
 *  container = FileSignature {
 *    file = f1.kt
 *  }
 *  inner = CommonSignature {
 *    packageFqn = 'foo.bar'
 *    classFqn = 'P'
 *    hashCode = null
 *    flags = 0
 *  }
 * }
 *
 *
 * CompositeSignature {
 *  container = FileSignature {
 *    file = f1.kt
 *  }
 *  inner = CommonSignature {
 *    packageFqn = 'foo.bar'
 *    classFqn = 'qux'
 *    hashCode = 0x....
 *    flags = 0
 *  }
 * }
 * ```
 *
 * So, here we need a special factor `file` that allows us to distinguish signatures with the same FQN in different files.
 * In a klib such file-private declarations can’t be referenced from files other than the file where the private declaration is declared in.
 *
 * The last case is a local declaration:
 * ```kotlin
 * package foo.bar
 * fun qux() {
 *   class L {
 *     fun meh() { .. }
 *   }
 * }
 * ```
 *
 * Here the signatures are going to be:
 * ```
 * CommonSignature { // qux()
 *   packageFqn = 'foo.bar'
 *   classFqn = 'qux'
 *   hashCode = 0x....
 *   flags = 0
 * }
 *
 * FileLocalSignature { // class L
 *   container = CommonSignature { // qux()
 *     packageFqn = 'foo.bar'
 *     classFqn = 'qux'
 *     hashCode = 0x....
 *     flags = 0
 *   }
 *   id = 0
 * }
 *
 * FileLocalSignature { // L.meh()
 *   container = FileLocalSignature { // class L
 *     container = CommonSignature { // qux()
 *       packageFqn = 'foo.bar'
 *       classFqn = 'qux'
 *       hashCode = 0x....
 *       flags = 0
 *     }
 *     id = 0
 *   }
 *   id = 0x123... // hash of L.meh() signature
 * }
 * ```
 */
sealed class IdSignature {

    /**
     * Used for some special kinds of declarations like `expect`, or synthetic Java accessors.
     */
    enum class Flags(val recursive: Boolean) {
        IS_EXPECT(true),
        IS_JAVA_FOR_KOTLIN_OVERRIDE_PROPERTY(false),
        IS_NATIVE_INTEROP_LIBRARY(true),
        IS_SYNTHETIC_JAVA_PROPERTY(false);

        fun encode(isSet: Boolean): Long = if (isSet) 1L shl ordinal else 0L
        fun decode(flags: Long): Boolean = (flags and (1L shl ordinal) != 0L)
    }

    /**
     * Whether the signature has cross-module visibility.
     */
    abstract val isPubliclyVisible: Boolean

    open fun isPackageSignature(): Boolean = false

    abstract fun topLevelSignature(): IdSignature
    abstract fun nearestPublicSig(): IdSignature

    abstract fun packageFqName(): FqName

    open fun asPublic(): CommonSignature? = null

    @Deprecated(
        "Rendering of signatures has been extracted to IdSignatureRenderer.render()",
        replaceWith = ReplaceWith("render()", "org.jetbrains.kotlin.ir.util.render"),
        level = DeprecationLevel.HIDDEN
    )
    fun render(): String = newRender()
    final override fun toString() = newRender()

    fun Flags.test(): Boolean = decode(flags())

    protected open fun flags(): Long = 0

    open val hasTopLevel: Boolean get() = !isPackageSignature()

    open val isLocal: Boolean get() = !isPubliclyVisible

    // TODO: this API is a bit hacky, consider to act somehow else
    open val visibleCrossFile: Boolean get() = true

    /**
     * This signature corresponds to a publicly accessible Kotlin declaration.
     *
     * @property description This property does not affect linkage and is used only for showing human-readable error messages.
     * Note: currently, we store here the mangled name from which [id] was computed. Later we can reconsider.
     */
    class CommonSignature(
        val packageFqName: String,
        val declarationFqName: String,
        val id: Long?,
        val mask: Long,
        val description: String?,
    ) : IdSignature() {

        @Deprecated(
            "When constructing 'CommonSignature', you need to set 'description' to the mangled name from which 'id' was " +
                    "computed, or to null if it's not applicable",
            level = DeprecationLevel.WARNING,
        )
        constructor(
            packageFqName: String,
            declarationFqName: String,
            id: Long?,
            mask: Long,
        ) : this(packageFqName, declarationFqName, id, mask, null)

        override val isPubliclyVisible: Boolean get() = true

        override fun packageFqName(): FqName = FqName(packageFqName)

        val shortName: String get() = declarationFqName.substringAfterLast('.')

        val firstNameSegment: String get() = declarationFqName.substringBefore('.')

        val nameSegments: List<String> get() = declarationFqName.split('.')

        private fun adaptMask(old: Long): Long =
            old xor Flags.values().fold(0L) { a, f ->
                if (!f.recursive) a or (old and (1L shl f.ordinal))
                else a
            }

        override fun topLevelSignature(): IdSignature {
            if (declarationFqName.isEmpty()) {
                assert(id == null)
                // package signature
                return this
            }

            val nameSegments = nameSegments
            val adaptedMask = adaptMask(mask)
            if (nameSegments.size == 1 && mask == adaptedMask) return this

            return CommonSignature(
                packageFqName = packageFqName,
                declarationFqName = nameSegments.first(),
                id = null,
                mask = adaptedMask,
                description = null
            )
        }

        override fun isPackageSignature(): Boolean = id == null && declarationFqName.isEmpty()

        override fun nearestPublicSig(): CommonSignature = this

        override fun flags(): Long = mask

        override fun asPublic(): CommonSignature = this

        override fun equals(other: Any?): Boolean =
            other is CommonSignature && packageFqName == other.packageFqName && declarationFqName == other.declarationFqName &&
                    id == other.id && mask == other.mask

        private val hashCode = ((packageFqName.hashCode() * 31 + declarationFqName.hashCode()) * 31 + id.hashCode()) * 31 + mask.hashCode()

        override fun hashCode(): Int = hashCode
    }

    /**
     * This signature is a container that contains 2 signatures ([container] and [inner] parts)
     */
    class CompositeSignature(val container: IdSignature, val inner: IdSignature) : IdSignature() {
        override val isPubliclyVisible: Boolean
            get() = true

        override val isLocal: Boolean
            get() = inner.isLocal

        override val visibleCrossFile: Boolean
            get() = container.visibleCrossFile

        override fun topLevelSignature(): IdSignature {
            return if (container is FileSignature)
                CompositeSignature(container, inner.topLevelSignature())
            else container.topLevelSignature()
        }

        override fun nearestPublicSig(): IdSignature {
            return if (container is FileSignature) inner.nearestPublicSig() else container.nearestPublicSig()
        }

        override fun packageFqName(): FqName {
            return if (container is FileSignature) inner.packageFqName() else container.packageFqName()
        }

        override fun equals(other: Any?): Boolean = other is CompositeSignature && container == other.container && inner == other.inner

        override fun hashCode(): Int = container.hashCode() * 31 + inner.hashCode()

    }

    /**
     * This is a special signature to reference accessors of a property.
     * It differs from [CommonSignature] because otherwise it’s not clear how to compute the container.
     *
     * TODO: Could be replaced with [CompositeSignature]
     *
     * @property propertySignature A signature of the property that this accessor corresponds to.
     * @property accessorSignature A signature of the accessor function for the property.
     */
    class AccessorSignature(val propertySignature: IdSignature, val accessorSignature: CommonSignature) : IdSignature() {
        override val isPubliclyVisible: Boolean get() = true

        override fun topLevelSignature(): IdSignature = propertySignature.topLevelSignature()

        override fun nearestPublicSig(): IdSignature = this

        override fun packageFqName(): FqName = propertySignature.packageFqName()

        override fun flags(): Long = accessorSignature.mask

        override fun asPublic(): CommonSignature = accessorSignature

        override fun equals(other: Any?): Boolean =
            if (other is AccessorSignature) accessorSignature == other.accessorSignature
            else accessorSignature == other

        private val hashCode = accessorSignature.hashCode()

        override fun hashCode(): Int = hashCode
    }

    /**
     * Used to represent a file which is required in case of top-level private declarations.
     * This signature is needed because 2 different top-level private declarations can have the same FQN.
     *
     * This signature is not navigatable through files.
     *
     * @property id A unique object to differentiate two file signatures with the same names. For example, an [IrFileSymbol].
     * @property fqName The name of the package this file belongs to.
     * @property fileName The name/path of the file entry.
     */
    class FileSignature(
        private val id: Any,
        private val fqName: FqName,
        val fileName: String
    ) : IdSignature() {

        constructor(fileSymbol: IrFileSymbol) : this(
            fileSymbol, fileSymbol.owner.packageFqName, fileSymbol.owner.fileEntry.name
        )

        override fun equals(other: Any?): Boolean = other is FileSignature && id == other.id

        override fun hashCode(): Int = id.hashCode()

        override val isPubliclyVisible: Boolean
            get() = true

        override val visibleCrossFile: Boolean
            get() = false

        override fun isPackageSignature(): Boolean = true

        override fun topLevelSignature(): IdSignature = this

        override fun nearestPublicSig(): IdSignature {
            error("Should not reach here ($this)")
        }

        override fun packageFqName(): FqName = fqName

        override val hasTopLevel: Boolean
            get() = false
    }

    /**
     * This signature represents some internal part of a declaration, like a backing field or a type parameter.
     *
     * This signature is not navigatable through files.
     */
    class LocalSignature(val localFqn: String, val hashSig: Long?, val description: String?) : IdSignature() {
        override val isPubliclyVisible: Boolean
            get() = false

        override val isLocal: Boolean
            get() = true

        fun index(): Int = hashSig?.toInt() ?: error("Expected index in $this")

        override fun topLevelSignature(): IdSignature {
            error("Illegal access: Local Sig does not have toplevel ($this")
        }

        override fun nearestPublicSig(): IdSignature {
            error("Illegal access: Local Sig does not have information about its public part ($this")
        }

        override fun packageFqName(): FqName {
            error("Illegal access: Local signature does not have package ($this")
        }

        override fun equals(other: Any?): Boolean {
            return other is LocalSignature && localFqn == other.localFqn && hashSig == other.hashSig
        }

        override fun hashCode(): Int {
            return (hashSig ?: 0L).toInt() * 31 + localFqn.hashCode()
        }
    }

    /**
     * [KT-42020](https://youtrack.jetbrains.com/issue/KT-42020)
     *
     * This special signature is required to disambiguate fake overrides 'foo(x: T)[T = String]' and 'foo(x: String)' in the code below:
     *
     * ```kotlin
     * open class Base<T> {
     *     fun foo(x: T) {}
     *     fun foo(x: String) {}
     * }
     *
     * class Derived : Base<String>()
     * ```
     *
     * NB: A similar clash is possible for generic member extension properties as well
     *
     * For each fake override `foo` we collect non-fake overrides overridden by `foo`
     * such that their value parameter types contain type parameters of 'Base',
     * sorted by the fully-qualified name of the containing class.
     *
     * NB: this special case of [IdSignature] is JVM-specific.
     */
    class SpecialFakeOverrideSignature(
        val memberSignature: IdSignature,
        val overriddenSignatures: List<IdSignature>
    ) : IdSignature() {
        override val isPubliclyVisible: Boolean
            get() = memberSignature.isPubliclyVisible

        override fun asPublic(): CommonSignature? = memberSignature.asPublic()

        override fun topLevelSignature(): IdSignature =
            memberSignature.topLevelSignature()

        override fun nearestPublicSig(): IdSignature =
            if (memberSignature.isPubliclyVisible)
                this
            else
                memberSignature.nearestPublicSig()

        override fun packageFqName(): FqName =
            memberSignature.packageFqName()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SpecialFakeOverrideSignature

            if (memberSignature != other.memberSignature) return false
            if (overriddenSignatures != other.overriddenSignatures) return false

            return true
        }

        private val hashCode = 31 * memberSignature.hashCode() + overriddenSignatures.hashCode()

        override fun hashCode(): Int = hashCode
    }

    /**
     * A deprecated signature used for local classes and their members to make it possible to build fake overridden declarations for such
     * local classes.
     *
     * This signature is not navigatable through files.
     *
     * @property id A hash of the member’s signature, not an ordered index, because it has to be stable.
     */
    class FileLocalSignature(val container: IdSignature, val id: Long, val description: String? = null) : IdSignature() {
        override val isPubliclyVisible: Boolean get() = false

        override fun packageFqName(): FqName = container.packageFqName()

        override val visibleCrossFile: Boolean
            get() = false

        override fun topLevelSignature(): IdSignature {
            val topLevelContainer = container.topLevelSignature()
            if (topLevelContainer === container) {
                if (topLevelContainer is CommonSignature && topLevelContainer.declarationFqName.isEmpty()) {
                    // private top level
                    return this
                }
            }
            return topLevelContainer
        }

        override fun nearestPublicSig(): IdSignature = container.nearestPublicSig()

        override fun equals(other: Any?): Boolean =
            other is FileLocalSignature && id == other.id && container == other.container

        private val hashCode = container.hashCode() * 31 + id.hashCode()

        override fun hashCode(): Int = hashCode
    }

    /**
     * Used to reference local declarations like a variable, value parameters, or anonymous initializers.
     *
     * This signature is not navigatable through files.
     */
    class ScopeLocalDeclaration(val id: Int, val description: String? = null) : IdSignature() {

        override val isPubliclyVisible: Boolean get() = false

        override val visibleCrossFile: Boolean
            get() = false

        override val hasTopLevel: Boolean get() = false

        override fun topLevelSignature(): IdSignature = error("Is not supported for Local ID")

        override fun nearestPublicSig(): IdSignature = error("Is not supported for Local ID")

        override fun packageFqName(): FqName = error("Is not supported for Local ID")

        override fun equals(other: Any?): Boolean =
            other is ScopeLocalDeclaration && id == other.id

        override fun hashCode(): Int = id
    }

    /**
     * A special signature to reference lowered declarations in PIR incremental cache.
     */
    class LoweredDeclarationSignature(val original: IdSignature, val stage: Int, val index: Int) : IdSignature() {
        override val isPubliclyVisible: Boolean get() = original.isPubliclyVisible

        override val hasTopLevel: Boolean get() = true

        override val visibleCrossFile: Boolean
            get() = original.visibleCrossFile

        override fun topLevelSignature(): IdSignature = this

        override fun nearestPublicSig(): IdSignature = this

        override fun packageFqName(): FqName = original.packageFqName()

        override fun equals(other: Any?): Boolean {
            return other is LoweredDeclarationSignature && original == other.original && stage == other.stage && index == other.index
        }

        private val hashCode = (index * 31 + stage) * 31 + original.hashCode()

        override fun hashCode(): Int = hashCode
    }
}

interface IdSignatureComposer {
    fun composeSignature(descriptor: DeclarationDescriptor): IdSignature?
    fun composeEnumEntrySignature(descriptor: ClassDescriptor): IdSignature?
    fun composeFieldSignature(descriptor: PropertyDescriptor): IdSignature?
    fun composeAnonInitSignature(descriptor: ClassDescriptor): IdSignature?

    fun withFileSignature(fileSignature: IdSignature.FileSignature, body: () -> Unit)

    val mangler: KotlinMangler.DescriptorMangler
}
