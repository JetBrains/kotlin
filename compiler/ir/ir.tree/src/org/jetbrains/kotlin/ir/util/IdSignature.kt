/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName

sealed class IdSignature {

    enum class Flags(val recursive: Boolean) {
        IS_EXPECT(true),
        IS_JAVA_FOR_KOTLIN_OVERRIDE_PROPERTY(false),
        IS_NATIVE_INTEROP_LIBRARY(true);

        fun encode(isSet: Boolean): Long = if (isSet) 1L shl ordinal else 0L
        fun decode(flags: Long): Boolean = (flags and (1L shl ordinal) != 0L)
    }

    abstract val isPublic: Boolean

    open fun isPackageSignature(): Boolean = false

    abstract fun topLevelSignature(): IdSignature
    abstract fun nearestPublicSig(): IdSignature

    abstract fun packageFqName(): FqName

    open fun asPublic(): PublicSignature? = null

    abstract fun render(): String

    fun Flags.test(): Boolean = decode(flags())

    protected open fun flags(): Long = 0

    open val hasTopLevel: Boolean get() = !isPackageSignature()

    val isLocal: Boolean get() = !isPublic

    override fun toString(): String =
        "${if (isPublic) "public" else "private"} ${render()}"

    class PublicSignature(val packageFqName: String, val declarationFqName: String, val id: Long?, val mask: Long) : IdSignature() {
        override val isPublic: Boolean get() = true

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
            if (nameSegments.size == 1) return this

            return PublicSignature(packageFqName, nameSegments.first(), null, adaptMask(mask))
        }

        override fun isPackageSignature(): Boolean = id == null && declarationFqName.isEmpty()

        override fun nearestPublicSig(): PublicSignature = this

        override fun flags(): Long = mask

        override fun render(): String = "$packageFqName/$declarationFqName|$id[${mask.toString(2)}]"

        override fun asPublic(): PublicSignature? = this

        override fun equals(other: Any?): Boolean =
            other is PublicSignature && packageFqName == other.packageFqName && declarationFqName == other.declarationFqName &&
                    id == other.id && mask == other.mask

        override fun hashCode(): Int =
            ((packageFqName.hashCode() * 31 + declarationFqName.hashCode()) * 31 + id.hashCode()) * 31 + mask.hashCode()
    }

    class AccessorSignature(val propertySignature: IdSignature, val accessorSignature: PublicSignature) : IdSignature() {
        override val isPublic: Boolean get() = true

        override fun topLevelSignature(): IdSignature = propertySignature.topLevelSignature()

        override fun nearestPublicSig(): IdSignature = this

        override fun packageFqName(): FqName = propertySignature.packageFqName()

        override fun render(): String = accessorSignature.render()

        override fun flags(): Long = accessorSignature.mask

        override fun asPublic(): PublicSignature? = accessorSignature

        override fun equals(other: Any?): Boolean =
            if (other is AccessorSignature) accessorSignature == other.accessorSignature
            else accessorSignature == other

        override fun hashCode(): Int = accessorSignature.hashCode()
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
        override val isPublic: Boolean
            get() = memberSignature.isPublic

        override fun topLevelSignature(): IdSignature =
            memberSignature.topLevelSignature()

        override fun nearestPublicSig(): IdSignature =
            if (memberSignature.isPublic)
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

        override fun hashCode(): Int {
            var result = memberSignature.hashCode()
            result = 31 * result + overriddenSignatures.hashCode()
            return result
        }
    }

    class FileLocalSignature(val container: IdSignature, val id: Long) : IdSignature() {
        override val isPublic: Boolean get() = false

        override fun packageFqName(): FqName = container.packageFqName()

        override fun topLevelSignature(): IdSignature {
            val topLevelContainer = container.topLevelSignature()
            if (topLevelContainer === container) {
                if (topLevelContainer is PublicSignature && topLevelContainer.declarationFqName.isEmpty()) {
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

        override fun hashCode(): Int = container.hashCode() * 31 + id.hashCode()
    }

    // Used to reference local variable and value parameters in function
    class ScopeLocalDeclaration(val id: Int, val description: String = "<no description>") : IdSignature() {
        override val isPublic: Boolean get() = false

        override val hasTopLevel: Boolean get() = false

        override fun topLevelSignature(): IdSignature = error("Is not supported for Local ID")

        override fun nearestPublicSig(): IdSignature = error("Is not supported for Local ID")

        override fun packageFqName(): FqName = error("Is not supported for Local ID")

        override fun render(): String = "#$id"

        override fun equals(other: Any?): Boolean =
            other is ScopeLocalDeclaration && id == other.id

        override fun hashCode(): Int = id
    }
}

interface IdSignatureComposer {
    fun composeSignature(descriptor: DeclarationDescriptor): IdSignature?
    fun composeEnumEntrySignature(descriptor: ClassDescriptor): IdSignature?
}
