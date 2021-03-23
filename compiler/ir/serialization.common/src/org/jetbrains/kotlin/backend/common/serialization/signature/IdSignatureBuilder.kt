/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName

abstract class IdSignatureBuilder<D> {
    protected var packageFqn: FqName = FqName.ROOT
    protected val classFqnSegments = mutableListOf<String>()
    protected var hashId: Long? = null
    protected var hashIdAcc: Long? = null
    protected var overridden: List<D>? = null
    protected var mask = 0L
    protected var container: IdSignature? = null
    protected var description: String? = null
    protected var errorIndex: Int? = null

    protected var isTopLevelPrivate: Boolean = false


    private var fileStorage: IdSignature.FileSignature? = null

    private fun ssssetFile(sf: IdSignature.FileSignature?) {
        if (sf == null) {
            println("jdhfjdsk")
        }
        fileStorage = sf
    }

    protected abstract val currentFileSignature: IdSignature.FileSignature?

    protected abstract fun accept(d: D)

    protected fun reset(resetContainer: Boolean = true) {
        this.packageFqn = FqName.ROOT
        this.classFqnSegments.clear()
        this.hashId = null
        this.hashIdAcc = null
        this.mask = 0L
        this.overridden = null
        this.description = null
        this.isTopLevelPrivate = false
        this.errorIndex = null

        if (resetContainer) container = null
    }


    protected fun buildContainerSignature(container: IdSignature): IdSignature.CompositeSignature {
        assert(packageFqn.isRoot)
        val localName = classFqnSegments.joinToString(".")
        val localHash = hashId
        return IdSignature.CompositeSignature(container, IdSignature.LocalSignature(localName, localHash, description))
    }

    protected fun build(): IdSignature {
        val packageFqName = packageFqn.asString()
        val classFqName = classFqnSegments.joinToString(".")
        return when {
            overridden != null -> {
                val preserved = overridden!!
                overridden = null
                val memberSignature = build()
                val overriddenSignatures = preserved.map { buildSignature(it) }
                return IdSignature.SpecialFakeOverrideSignature(memberSignature, overriddenSignatures)
            }
            isTopLevelPrivate -> {
                val fileSig = currentFileSignature ?:
                error("File expected to be not null")
                isTopLevelPrivate = false
                IdSignature.CompositeSignature(fileSig, build())
            }
            errorIndex != null -> {
                val fileSig = currentFileSignature ?:
                error("File expected to be not null")
                IdSignature.CompositeSignature(fileSig, IdSignature.ScopeLocalDeclaration(errorIndex!!, description))
            }
            container != null -> {
                val preservedContainer = container!!
                container = null
                buildContainerSignature(preservedContainer)
            }

            hashIdAcc == null -> {
                IdSignature.PublicSignature(packageFqName, classFqName, hashId, mask)
            }
            else -> {
                val accessorSignature = IdSignature.PublicSignature(packageFqName, classFqName, hashIdAcc, mask)
                hashIdAcc = null
                classFqnSegments.run { removeAt(lastIndex) }
                val propertySignature = build()
                IdSignature.AccessorSignature(propertySignature, accessorSignature)
            }
        }
    }

    protected fun setExpected(f: Boolean) {
        mask = mask or IdSignature.Flags.IS_EXPECT.encode(f)
    }

    protected fun setSpecialJavaProperty(f: Boolean) {
        mask = mask or IdSignature.Flags.IS_JAVA_FOR_KOTLIN_OVERRIDE_PROPERTY.encode(f)
    }

    protected fun setSyntheticJavaProperty(f: Boolean) {
        mask = mask or IdSignature.Flags.IS_SYNTHETIC_JAVA_PROPERTY.encode(f)
    }

    protected open fun platformSpecificModule(descriptor: ModuleDescriptor) {
        error("Should not reach here with $descriptor")
    }

    protected open fun platformSpecificProperty(descriptor: PropertyDescriptor) {}
    protected open fun platformSpecificGetter(descriptor: PropertyGetterDescriptor) {}
    protected open fun platformSpecificSetter(descriptor: PropertySetterDescriptor) {}
    protected open fun platformSpecificFunction(descriptor: FunctionDescriptor) {}
    protected open fun platformSpecificConstructor(descriptor: ConstructorDescriptor) {}
    protected open fun platformSpecificClass(descriptor: ClassDescriptor) {}
    protected open fun platformSpecificAlias(descriptor: TypeAliasDescriptor) {}
    protected open fun platformSpecificPackage(descriptor: PackageFragmentDescriptor) {}

    protected open fun isKotlinPackage(descriptor: PackageFragmentDescriptor): Boolean = true

    fun buildSignature(declaration: D, extra: IdSignatureBuilder<D>.() -> Unit = { }): IdSignature {
        reset()

        accept(declaration)
        extra()

        return build()
    }
}