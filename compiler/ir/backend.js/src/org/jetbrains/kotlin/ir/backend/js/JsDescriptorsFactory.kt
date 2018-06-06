/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.js

import org.jetbrains.kotlin.backend.common.descriptors.DescriptorsFactory
import org.jetbrains.kotlin.builtins.CompanionObjectMapping.isMappedIntrinsicCompanionObject
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.createValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.name.Name
import java.util.*

class JsDescriptorsFactory : DescriptorsFactory {
    private val singletonFieldDescriptors = HashMap<IrBindableSymbol<*, *>, IrFieldSymbol>()
    private val outerThisFieldSymbols = HashMap<IrClass, IrFieldSymbol>()
    private val innerClassConstructors = HashMap<IrConstructorSymbol, IrConstructorSymbol>()

    override fun getSymbolForEnumEntry(enumEntry: IrEnumEntrySymbol): IrFieldSymbol = TODO()

    override fun getOuterThisFieldSymbol(innerClass: IrClass): IrFieldSymbol =
        if (!innerClass.isInner) throw AssertionError("Class is not inner: ${innerClass.dump()}")
        else outerThisFieldSymbols.getOrPut(innerClass) {
            val outerClass = innerClass.parent as? IrClass
                    ?: throw AssertionError("No containing class for inner class ${innerClass.dump()}")

            IrFieldSymbolImpl(PropertyDescriptorImpl.create(
                innerClass.descriptor,
                Annotations.EMPTY,
                Modality.FINAL,
                Visibilities.PROTECTED,
                false,
                Name.identifier("\$this"),
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE,
                false,
                true,
                false,
                false,
                false,
                false
            ).apply {
                setType(
                    outerClass.defaultType,
                    emptyList(),
                    innerClass.descriptor.thisAsReceiverParameter,
                    null as? ReceiverParameterDescriptor
                )
                initialize(null, null)
            })
        }


    override fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: IrConstructor): IrConstructorSymbol {
        val innerClass = innerClassConstructor.parent as IrClass
        assert(innerClass.isInner) { "Class is not inner: $innerClass" }

        return innerClassConstructors.getOrPut(innerClassConstructor.symbol) {
            createInnerClassConstructorWithOuterThisParameter(innerClassConstructor.descriptor)
        }
    }

    private fun createInnerClassConstructorWithOuterThisParameter(oldDescriptor: ClassConstructorDescriptor): IrConstructorSymbol {
        val classDescriptor = oldDescriptor.containingDeclaration
        val outerThisType = (classDescriptor.containingDeclaration as ClassDescriptor).defaultType

        val newDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
            classDescriptor, oldDescriptor.annotations, oldDescriptor.isPrimary, oldDescriptor.source
        )

        val outerThisValueParameter = createValueParameter(newDescriptor, 0, Namer.OUTER_NAME, outerThisType)

        val newValueParameters =
            listOf(outerThisValueParameter) +
                    oldDescriptor.valueParameters.map { it.copy(newDescriptor, it.name, it.index + 1) }
        newDescriptor.initialize(newValueParameters, oldDescriptor.visibility)
        newDescriptor.returnType = oldDescriptor.returnType
        return IrConstructorSymbolImpl(newDescriptor)
    }

    override fun getSymbolForObjectInstance(singleton: IrClassSymbol): IrFieldSymbol =
        singletonFieldDescriptors.getOrPut(singleton) {
            IrFieldSymbolImpl(createObjectInstanceFieldDescriptor(singleton.descriptor))
        }

    private fun createObjectInstanceFieldDescriptor(objectDescriptor: ClassDescriptor): PropertyDescriptor {
        assert(objectDescriptor.kind == ClassKind.OBJECT) { "Should be an object: $objectDescriptor" }

        val isNotMappedCompanion = objectDescriptor.isCompanionObject && !isMappedIntrinsicCompanionObject(objectDescriptor)
        val name = if (isNotMappedCompanion) objectDescriptor.name else Name.identifier("INSTANCE")
        val containingDeclaration = if (isNotMappedCompanion) objectDescriptor.containingDeclaration else objectDescriptor
        return PropertyDescriptorImpl.create(
            containingDeclaration,
            Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC, false,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE, /* lateInit = */ false, /* isConst = */ false,
            /* isExpect = */ false, /* isActual = */ false, /* isExternal = */ false, /* isDelegated = */ false
        ).apply {
            setType(objectDescriptor.defaultType, emptyList(), null, null as ReceiverParameterDescriptor)
            initialize(null, null)
        }
    }
}
