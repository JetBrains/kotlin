/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.descriptors.IrPropertyDelegateDescriptor
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType

open class IdSignatureDescriptor(private val mangler: KotlinMangler.DescriptorMangler) : IdSignatureComposer {

    protected open fun createSignatureBuilder(): DescriptorBasedSignatureBuilder = DescriptorBasedSignatureBuilder(mangler)

    protected open inner class DescriptorBasedSignatureBuilder(private val mangler: KotlinMangler.DescriptorMangler) :
        IdSignatureBuilder<DeclarationDescriptor>(),
        DeclarationDescriptorVisitor<Unit, Nothing?> {

        override fun accept(d: DeclarationDescriptor) {
            d.accept(this, null)
        }

        private fun createContainer() {
            container = container?.let {
                buildContainerSignature(it)
            } ?: build()

            reset(false)
        }

        private fun reportUnexpectedDescriptor(descriptor: DeclarationDescriptor) {
            error("Unexpected descriptor $descriptor")
        }

        private fun setDescription(descriptor: DeclarationDescriptor) {
            if (container != null) {
                description = DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor)
            }
        }

        private fun resolveLocalName(descriptor: DeclarationDescriptor): String {
            val name = descriptor.name
            return buildString {
                if (!name.isAnonymous) append(name)
                append(MangleConstant.LOCAL_DECLARATION_INDEX_PREFIX)
                append(localScope?.declarationIndex(descriptor) ?: "0")
            }
        }

        private fun collectParents(descriptor: DeclarationDescriptorNonRoot, isLocal: Boolean) {
            descriptor.containingDeclaration.accept(this, null)

            if (isLocal) {
                createContainer()
                val localName = resolveLocalName(descriptor)
                classFqnSegments.add(localName)
            } else {
                classFqnSegments.add(descriptor.name.asString())
            }
        }

        private val DeclarationDescriptorWithVisibility.isLocal: Boolean
            get() = visibility == DescriptorVisibilities.LOCAL

        private val DeclarationDescriptorWithVisibility.isPrivate: Boolean
            get() = visibility == DescriptorVisibilities.PRIVATE

        private val DeclarationDescriptorWithVisibility.isTopLevelPrivate: Boolean
            get() = isPrivate && mangler.run { !isPlatformSpecificExport() } && containingDeclaration?.let { it is PackageFragmentDescriptor && isKotlinPackage(it) } ?: false

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Nothing?) {
            packageFqn = descriptor.fqName
            platformSpecificPackage(descriptor)
        }

        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Nothing?) {
            packageFqn = descriptor.fqName
        }

        override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: Nothing?) {
            reportUnexpectedDescriptor(descriptor)
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Nothing?) {
            collectParents(descriptor, descriptor.isLocal)
            isTopLevelPrivate = descriptor.isTopLevelPrivate
            hashId = mangler.run { descriptor.signatureMangle() }
            setDescription(descriptor)
            setExpected(descriptor.isExpect)
            platformSpecificFunction(descriptor)
        }

        override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: Nothing?) {
            descriptor.containingDeclaration.accept(this, null)
            createContainer()

            classFqnSegments.add(MangleConstant.TYPE_PARAMETER_MARKER_NAME)
            hashId = descriptor.index.toLong()
            description = DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor)
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Nothing?) {
            collectParents(descriptor, descriptor.isLocal)
            isTopLevelPrivate = descriptor.isTopLevelPrivate
            setDescription(descriptor)
            setExpected(descriptor.isExpect)
            platformSpecificClass(descriptor)
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Nothing?) {
            collectParents(descriptor, descriptor.isLocal)
            isTopLevelPrivate = descriptor.isTopLevelPrivate
            setExpected(descriptor.isExpect)
            platformSpecificAlias(descriptor)
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Nothing?) {
            platformSpecificModule(descriptor)
        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: Nothing?) {
            collectParents(constructorDescriptor, false.also { assert(!constructorDescriptor.isLocal) })
            hashId = mangler.run { constructorDescriptor.signatureMangle() }
            platformSpecificConstructor(constructorDescriptor)
        }

        override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: Nothing?) =
            visitClassDescriptor(scriptDescriptor, data)

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Nothing?) {
            collectParents(descriptor, descriptor.isLocal)
            val actualDeclaration = if (descriptor is IrPropertyDelegateDescriptor) {
                descriptor.correspondingProperty
            } else {
                descriptor.also {
                    isTopLevelPrivate = it.isTopLevelPrivate
                }
            }

            hashId = mangler.run { actualDeclaration.signatureMangle() }
            setExpected(actualDeclaration.isExpect)
            platformSpecificProperty(actualDeclaration)
        }

        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: Nothing?) {
            reportUnexpectedDescriptor(descriptor)
        }

        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: Nothing?) {
            descriptor.correspondingProperty.accept(this, null)
            hashIdAcc = mangler.run { descriptor.signatureMangle() }
            classFqnSegments.add(descriptor.name.asString())
            setExpected(descriptor.isExpect)
            platformSpecificGetter(descriptor)
        }

        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: Nothing?) {
            descriptor.correspondingProperty.accept(this, null)
            hashIdAcc = mangler.run { descriptor.signatureMangle() }
            classFqnSegments.add(descriptor.name.asString())
            setExpected(descriptor.isExpect)
            platformSpecificSetter(descriptor)
        }

        override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: Nothing?) {
            reportUnexpectedDescriptor(descriptor)
        }

        override val currentFileSignature: IdSignature.FileSignature?
            get() = currentFileSignatureX
    }

    private val composer: DescriptorBasedSignatureBuilder by lazy { createSignatureBuilder() }

    override fun composeSignature(descriptor: DeclarationDescriptor): IdSignature {
        val sig = composer.buildSignature(descriptor)
        return sig
    }

    override fun composeEnumEntrySignature(descriptor: ClassDescriptor): IdSignature {
        return composer.buildSignature(descriptor)
    }

    override fun composeFieldSignature(descriptor: PropertyDescriptor): IdSignature {
        return composer.buildSignature(descriptor)
    }

    override fun composeAnonInitSignature(descriptor: ClassDescriptor): IdSignature {
        return composer.buildSignature(descriptor)
    }

    private class LocalScope(val parent: LocalScope?) : SignatureScope<DeclarationDescriptor> {

        private var classIndex = 0
        private var functionIndex = 0
        private var anonymousFunIndex = 0
        private var anonymousClassIndex = 0


        override fun commitLambda(descriptor: DeclarationDescriptor) {
            assert(descriptor is FunctionDescriptor)
            scopeDeclarationsIndexMap[descriptor] = anonymousFunIndex++
        }

        override fun commitLocalFunction(descriptor: DeclarationDescriptor) {
            assert(descriptor is FunctionDescriptor)
            scopeDeclarationsIndexMap[descriptor] = functionIndex++
        }

        override fun commitAnonymousObject(descriptor: DeclarationDescriptor) {
            assert(descriptor is ClassDescriptor)
            scopeDeclarationsIndexMap[descriptor] = anonymousClassIndex++
        }

        override fun commitLocalClass(descriptor: DeclarationDescriptor) {
            assert(descriptor is ClassDescriptor)
            scopeDeclarationsIndexMap[descriptor] = classIndex++
        }

        val scopeDeclarationsIndexMap: MutableMap<DeclarationDescriptor, Int> = mutableMapOf()

        fun declarationIndex(descriptor: DeclarationDescriptor): Int? {
            assert(DescriptorUtils.isLocal(descriptor))
            return scopeDeclarationsIndexMap[descriptor] ?: parent?.declarationIndex(descriptor)
        }
    }

    private var localScope: LocalScope? = null

    private var currentFileSignatureX: IdSignature.FileSignature? = null

    override fun setupTypeApproximation(app: (KotlinType) -> KotlinType) {
        mangler.setupTypeApproximation(app)
    }


    override fun <E, R> inLocalScope(builder: (SignatureScope<E>) -> Unit, block: () -> R): R {
        val newScope = LocalScope(localScope)
        localScope = newScope

        @Suppress("UNCHECKED_CAST")
        val casted = builder as ((SignatureScope<DeclarationDescriptor>) -> Unit)

        casted(newScope)

        val result = block()

        localScope = newScope.parent

        return result
    }

    override fun inFile(file: IrFileSymbol?, block: () -> Unit) {
        currentFileSignatureX = file?.let { IdSignature.FileSignature(it) }

        block()

        currentFileSignatureX = null
    }

}