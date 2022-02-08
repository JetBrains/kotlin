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

sealed class IdSignature {

    enum class Flags(val recursive: Boolean) {
        IS_EXPECT(true),
        IS_JAVA_FOR_KOTLIN_OVERRIDE_PROPERTY(false),
        IS_NATIVE_INTEROP_LIBRARY(true),
        IS_SYNTHETIC_JAVA_PROPERTY(false);

        fun encode(isSet: Boolean): Long = if (isSet) 1L shl ordinal else 0L
        fun decode(flags: Long): Boolean = (flags and (1L shl ordinal) != 0L)
    }

    /**
     * Means that signature has cross-module visibility. In other words referencing declaration could found in klib if property is `true`
     */
    abstract val isPubliclyVisible: Boolean

    open fun isPackageSignature(): Boolean = false

    abstract fun topLevelSignature(): IdSignature
    abstract fun nearestPublicSig(): IdSignature

    abstract fun packageFqName(): FqName

    open fun asPublic(): CommonSignature? = null

    abstract fun render(): String

    fun Flags.test(): Boolean = decode(flags())

    protected open fun flags(): Long = 0

    open val hasTopLevel: Boolean get() = !isPackageSignature()

    open val isLocal: Boolean get() = !isPubliclyVisible

    // TODO: this API is a bit hacky, consider to act somehow else
    open val visibleCrossFile: Boolean get() = true

    override fun toString(): String =
        "${if (isLocal) "local " else ""}${render()}"

    class CommonSignature(val packageFqName: String, val declarationFqName: String, val id: Long?, val mask: Long) : IdSignature() {
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

            return CommonSignature(packageFqName, nameSegments.first(), null, adaptedMask)
        }

        override fun isPackageSignature(): Boolean = id == null && declarationFqName.isEmpty()

        override fun nearestPublicSig(): CommonSignature = this

        override fun flags(): Long = mask

        override fun render(): String = "$packageFqName/$declarationFqName|$id[${mask.toString(2)}]"

        override fun asPublic(): CommonSignature = this

        override fun equals(other: Any?): Boolean =
            other is CommonSignature && packageFqName == other.packageFqName && declarationFqName == other.declarationFqName &&
                    id == other.id && mask == other.mask

        private val hashCode = ((packageFqName.hashCode() * 31 + declarationFqName.hashCode()) * 31 + id.hashCode()) * 31 + mask.hashCode()

        override fun hashCode(): Int = hashCode
    }

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

        override fun render(): String {
            return buildString {
                append("[ ")
                append(container)
                append(" <- ")
                append(inner)
                append(" ]")
            }
        }

        override fun equals(other: Any?): Boolean = other is CompositeSignature && container == other.container && inner == other.inner

        override fun hashCode(): Int = container.hashCode() * 31 + inner.hashCode()

    }

    class AccessorSignature(val propertySignature: IdSignature, val accessorSignature: CommonSignature) : IdSignature() {
        override val isPubliclyVisible: Boolean get() = true

        override fun topLevelSignature(): IdSignature = propertySignature.topLevelSignature()

        override fun nearestPublicSig(): IdSignature = this

        override fun packageFqName(): FqName = propertySignature.packageFqName()

        override fun render(): String = accessorSignature.render()

        override fun flags(): Long = accessorSignature.mask

        override fun asPublic(): CommonSignature = accessorSignature

        override fun equals(other: Any?): Boolean =
            if (other is AccessorSignature) accessorSignature == other.accessorSignature
            else accessorSignature == other

        private val hashCode = accessorSignature.hashCode()

        override fun hashCode(): Int = hashCode
    }

    class FileSignature(
        private val id: Any,
        private val fqName: FqName,
        val fileName: String
    ) : IdSignature() {

        constructor(fileSymbol: IrFileSymbol) : this(
            fileSymbol, fileSymbol.owner.fqName, fileSymbol.owner.fileEntry.name
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

        override fun render(): String {
            return "File '$fileName'"
        }

        override val hasTopLevel: Boolean
            get() = false
    }

    class LocalSignature(val localFqn: String, val hashSig: Long?, val description: String?) : IdSignature() {
        override val isPubliclyVisible: Boolean
            get() = false

        override val isLocal: Boolean
            get() = true

        fun index(): Int = hashSig?.toInt() ?: error("Expected index in ${render()}")

        override fun topLevelSignature(): IdSignature {
            error("Illegal access: Local Sig does not have toplevel (${render()}")
        }

        override fun nearestPublicSig(): IdSignature {
            error("Illegal access: Local Sig does not have information about its public part (${render()}")
        }

        override fun packageFqName(): FqName {
            error("Illegal access: Local signature does not have package (${render()}")
        }

        override fun render(): String {
            return buildString {
                append("Local[")
                append(localFqn)
                hashSig?.let {
                    append(",")
                    append(it)
                }
                description?.let {
                    append(" | ")
                    append(it)
                }
                append("]")
            }
        }

        override fun equals(other: Any?): Boolean {
            return other is LocalSignature && localFqn == other.localFqn && hashSig == other.hashSig
        }

        override fun hashCode(): Int {
            return (hashSig ?: 0L).toInt() * 31 + localFqn.hashCode()
        }
    }

    // KT-42020
    // This special signature is required to disambiguate fake overrides 'foo(x: T)[T = String]' and 'foo(x: String)' in the code below:
    //
    //  open class Base<T> {
    //      fun foo(x: T) {}
    //      fun foo(x: String) {}
    //  }
    //
    //  class Derived : Base<String>()
    //
    // (NB similar clash is possible for generic member extension properties as well)
    //
    // For each fake override 'foo' we collect non-fake overrides overridden by 'foo'
    // such that their value parameter types contain type parameters of 'Base',
    // sorted by the fully-qualified name of the containing class.
    //
    // NB this special case of IdSignature is JVM-specific.
    class SpecialFakeOverrideSignature(
        val memberSignature: IdSignature,
        val overriddenSignatures: List<IdSignature>
    ) : IdSignature() {
        override val isPubliclyVisible: Boolean
            get() = memberSignature.isPubliclyVisible

        override fun topLevelSignature(): IdSignature =
            memberSignature.topLevelSignature()

        override fun nearestPublicSig(): IdSignature =
            if (memberSignature.isPubliclyVisible)
                this
            else
                memberSignature.nearestPublicSig()

        override fun packageFqName(): FqName =
            memberSignature.packageFqName()

        override fun render(): String =
            memberSignature.render()

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

        override fun render(): String = "${container.render()}:$id"

        override fun equals(other: Any?): Boolean =
            other is FileLocalSignature && id == other.id && container == other.container

        private val hashCode = container.hashCode() * 31 + id.hashCode()

        override fun hashCode(): Int = hashCode
    }

    // Used to reference local variable and value parameters in function
    class ScopeLocalDeclaration(val id: Int, _description: String? = null) : IdSignature() {

        val description: String = _description ?: "<no description>"

        override val isPubliclyVisible: Boolean get() = false

        override val visibleCrossFile: Boolean
            get() = false

        override val hasTopLevel: Boolean get() = false

        override fun topLevelSignature(): IdSignature = error("Is not supported for Local ID")

        override fun nearestPublicSig(): IdSignature = error("Is not supported for Local ID")

        override fun packageFqName(): FqName = error("Is not supported for Local ID")

        override fun render(): String = "#$id"

        override fun equals(other: Any?): Boolean =
            other is ScopeLocalDeclaration && id == other.id

        override fun hashCode(): Int = id
    }

    class LoweredDeclarationSignature(val original: IdSignature, val stage: Int, val index: Int) : IdSignature() {
        override val isPubliclyVisible: Boolean get() = original.isPubliclyVisible

        override val hasTopLevel: Boolean get() = true

        override val visibleCrossFile: Boolean
            get() = original.visibleCrossFile

        override fun topLevelSignature(): IdSignature = this

        override fun nearestPublicSig(): IdSignature = this

        override fun packageFqName(): FqName = original.packageFqName()

        override fun render(): String = "ic#$stage:${original.render()}-$index"

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
}
