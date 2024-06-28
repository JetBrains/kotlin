/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.jvm.EnumEntriesIntrinsicMappingsCache
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.findEnumValuesFunction
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name

// This class generates synthetic `$EntriesIntrinsicMappings` classes which cache the result of calling the `enumEntries` intrinsic
// on Java enums and on old (pre-1.9) Kotlin enums.
//
// `$EntriesIntrinsicMappings` classes are exactly the same as `$EntriesMappings` classes generated in `EnumExternalEntriesLowering`
// Unfortunately, we cannot reuse the logic from that lowering as is because, as opposed to the normal function call `E.entries`,
// the call to the intrinsic `enumEntries<E>()` is fully reified only during the codegen, where all IR of the module has already been
// lowered, notably:
// 1) All the `$EntriesMappings` have already been created for the `E.entries` calls, so we cannot easily reuse those classes, which is the
//    the reason why we're generating new classes with a slightly different name, even though it might be suboptimal in cases when you have
//    both `E.entries` and `enumEntries<E>` calls to the same enum in the same container.
// 2) Static initializers have been lowered, so we're generating the `<clinit>` method manually, as opposed to adding static init sections
//    as `EnumExternalEntriesLowering` does.
class EnumEntriesIntrinsicMappingsCacheImpl(private val context: JvmBackendContext) : EnumEntriesIntrinsicMappingsCache() {
    private val storage = mutableMapOf<IrClass, MappingsClass>()

    private inner class MappingsClass(val containingClass: IrClass) {
        val irClass = context.irFactory.buildClass {
            name = Name.identifier("EntriesIntrinsicMappings")
            origin = JvmLoweredDeclarationOrigin.ENUM_MAPPINGS_FOR_ENTRIES
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            parent = containingClass
        }

        val enums = hashMapOf<IrClass, IrField>()
    }

    @Synchronized
    override fun getEnumEntriesIntrinsicMappings(containingClass: IrClass, enumClass: IrClass): IrField {
        val mappingsClass = storage.getOrPut(containingClass) { MappingsClass(containingClass) }
        val field = mappingsClass.enums.getOrPut(enumClass) {
            mappingsClass.irClass.addField {
                name = Name.identifier("entries\$${mappingsClass.enums.size}")
                type = context.ir.symbols.enumEntries.typeWith(enumClass.defaultType)
                origin = JvmLoweredDeclarationOrigin.ENUM_MAPPINGS_FOR_ENTRIES
                isFinal = true
                isStatic = true
            }
        }
        return field
    }

    override fun generateMappingsClasses() {
        val backendContext = context
        for (klass in storage.values) {
            // Use the same origin and visibility for `<clinit>` as in StaticInitializersLowering.
            val clinit = klass.irClass.addFunction(
                "<clinit>", context.irBuiltIns.unitType, visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY,
                isStatic = true, origin = JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER
            )
            clinit.body = context.createIrBuilder(clinit.symbol).irBlockBody(clinit) {
                // Sort fields by name. Note that if there are a lot of calls to entries of different enums in the same container, it would
                // result in the ordering "entries$0, entries$1, entries$10, entries$11, entries$12, ...", but it's not a big deal.
                for ((enum, field) in klass.enums.entries.sortedBy { it.value.name }) {
                    // For each field, we're generating:
                    //     entries$N = kotlin.enums.EnumEntriesKt.enumEntries(E.values())
                    val enumValues = enum.findEnumValuesFunction(backendContext)
                    +irSetField(
                        null, field,
                        irCall(backendContext.ir.symbols.createEnumEntries).apply {
                            putValueArgument(0, irCall(enumValues))
                        }
                    )
                }
            }

            ClassCodegen.getOrCreate(klass.irClass, backendContext).generate()
        }
    }
}
