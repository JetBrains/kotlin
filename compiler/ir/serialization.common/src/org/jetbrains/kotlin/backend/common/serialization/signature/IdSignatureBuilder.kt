/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.name.FqName

abstract class IdSignatureBuilder<Declaration : Any, Mangler : KotlinMangler<Declaration>> {
    private data class PropertyAccessorIdHashAndDescription(val id: Long, val description: String)

    protected var packageFqn: FqName = FqName.ROOT
    protected val classFqnSegments = mutableListOf<String>()

    /**
     * Use [setHashIdAndDescriptionFor] or [setHashIdAndDescription] with `isPropertyAccessor = false` to set this property.
     *
     * This property is made private to enforce always setting [description] along with it.
     */
    private var hashId: Long? = null

    /**
     * Use [setHashIdAndDescriptionFor] or [setHashIdAndDescription] with `isPropertyAccessor = true` to set this property.
     *
     * This property is made private to enforce always setting [description] along with it.
     */
    private var propertyAccessorIdHashAndDescription: PropertyAccessorIdHashAndDescription? = null

    protected var overridden: List<Declaration>? = null
    protected var mask = 0L

    /**
     * For local or top-level private declarations, the signature of the containing declaration **or** [IdSignature.FileSignature]
     * respectively.
     *
     * Used to build [IdSignature.CompositeSignature].
     */
    protected var container: IdSignature? = null

    protected fun createContainer() {
        container = container?.let {
            buildContainerSignature(it)
        } ?: build()

        reset(false)
    }

    protected var description: String? = null

    protected abstract fun renderDeclarationForDescription(declaration: Declaration): String

    protected fun setDescriptionIfLocalDeclaration(declaration: Declaration) {
        if (container != null) {
            description = renderDeclarationForDescription(declaration)
        }
    }

    protected var isTopLevelPrivate: Boolean = false

    protected abstract val currentFileSignature: IdSignature.FileSignature?

    protected abstract val mangler: Mangler

    protected fun setHashIdAndDescriptionFor(declaration: Declaration, isPropertyAccessor: Boolean) {
        mangler.run {
            val mangledName = declaration.signatureString(compatibleMode = false)
            val id = mangledName.hashMangle
            setHashIdAndDescription(id, mangledName, isPropertyAccessor)
        }
    }

    protected fun setHashIdAndDescription(id: Long, description: String, isPropertyAccessor: Boolean) {
        if (isPropertyAccessor) {
            propertyAccessorIdHashAndDescription = PropertyAccessorIdHashAndDescription(id, description)
        } else {
            hashId = id
            this.description = description
        }
    }

    protected abstract fun accept(d: Declaration)

    protected fun reset(resetContainer: Boolean = true) {
        this.packageFqn = FqName.ROOT
        this.classFqnSegments.clear()
        this.hashId = null
        this.propertyAccessorIdHashAndDescription = null
        this.mask = 0L
        this.overridden = null
        this.description = null
        this.isTopLevelPrivate = false

        if (resetContainer) container = null
    }


    private fun buildContainerSignature(container: IdSignature): IdSignature.CompositeSignature {
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
                val fileSig = currentFileSignature
                    ?: error("File expected to be not null ($packageFqName, $classFqName)")
                isTopLevelPrivate = false
                IdSignature.CompositeSignature(fileSig, build())
            }
            container != null -> {
                val preservedContainer = container!!
                container = null
                buildContainerSignature(preservedContainer)
            }

            propertyAccessorIdHashAndDescription != null -> {
                val accessorSignature = IdSignature.CommonSignature(
                    packageFqName = packageFqName,
                    declarationFqName = classFqName,
                    id = propertyAccessorIdHashAndDescription!!.id,
                    mask = mask,
                    description = propertyAccessorIdHashAndDescription!!.description,
                )
                propertyAccessorIdHashAndDescription = null
                classFqnSegments.run { removeAt(lastIndex) }
                val propertySignature = build()
                IdSignature.AccessorSignature(propertySignature, accessorSignature)
            }
            else -> {
                IdSignature.CommonSignature(
                    packageFqName = packageFqName,
                    declarationFqName = classFqName,
                    id = hashId,
                    mask = mask,
                    description = description,
                )
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

    fun buildSignature(declaration: Declaration): IdSignature {
        reset()

        accept(declaration)

        return build()
    }
}