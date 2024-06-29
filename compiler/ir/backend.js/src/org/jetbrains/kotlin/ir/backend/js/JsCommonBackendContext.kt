/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.InlineClassesUtils
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isDispatchReceiver
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope

interface JsCommonBackendContext : CommonBackendContext {
    override val mapping: JsMapping

    val reflectionSymbols: ReflectionSymbols
    val propertyLazyInitialization: PropertyLazyInitialization

    override val inlineClassesUtils: JsCommonInlineClassesUtils

    val coroutineSymbols: JsCommonCoroutineSymbols

    val jsPromiseSymbol: IrClassSymbol?

    val catchAllThrowableType: IrType
        get() = irBuiltIns.throwableType

    val es6mode: Boolean
        get() = false

    val suiteFun: IrSimpleFunctionSymbol?
    val testFun: IrSimpleFunctionSymbol?

    val enumEntries: IrClassSymbol
    val createEnumEntries: IrSimpleFunctionSymbol

    val testFunsPerFile: HashMap<IrFile, IrSimpleFunction>

    fun createTestContainerFun(container: IrDeclaration): IrSimpleFunction {
        val irFile = container.file
        return testFunsPerFile.getOrPut(irFile) {
            irFactory.addFunction(irFile) {
                name = Name.identifier("test fun")
                returnType = irBuiltIns.unitType
                origin = JsIrBuilder.SYNTHESIZED_DECLARATION
            }.apply {
                body = irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, emptyList())
            }
        }
    }

    val externalPackageFragment: MutableMap<IrFileSymbol, IrFile>
    val additionalExportedDeclarations: Set<IrDeclaration>
    val bodilessBuiltInsPackageFragment: IrPackageFragment
}

// TODO: investigate if it could be removed
internal fun <T> BackendContext.lazy2(fn: () -> T) = lazy(LazyThreadSafetyMode.NONE) { irFactory.stageController.withInitialIr(fn) }

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JsCommonCoroutineSymbols(
    symbolTable: SymbolTable,
    val module: ModuleDescriptor,
    val context: JsCommonBackendContext
) {
    val coroutinePackage = module.getPackage(COROUTINE_PACKAGE_FQNAME)
    val coroutineIntrinsicsPackage = module.getPackage(COROUTINE_INTRINSICS_PACKAGE_FQNAME)

    val coroutineImpl =
        symbolTable.descriptorExtension.referenceClass(findClass(coroutinePackage.memberScope, COROUTINE_IMPL_NAME))

    val coroutineImplLabelPropertyGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("state")!!.owner }
    val coroutineImplLabelPropertySetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("state")!!.owner }
    val coroutineImplResultSymbolGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("result")!!.owner }
    val coroutineImplResultSymbolSetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("result")!!.owner }
    val coroutineImplExceptionPropertyGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("exception")!!.owner }
    val coroutineImplExceptionPropertySetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("exception")!!.owner }
    val coroutineImplExceptionStatePropertyGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("exceptionState")!!.owner }
    val coroutineImplExceptionStatePropertySetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("exceptionState")!!.owner }

    val continuationClass = symbolTable.descriptorExtension.referenceClass(
        coroutinePackage.memberScope.getContributedClassifier(
            CONTINUATION_NAME,
            NoLookupLocation.FROM_BACKEND
        ) as ClassDescriptor
    )

    val coroutineSuspendedGetter = symbolTable.descriptorExtension.referenceSimpleFunction(
        coroutineIntrinsicsPackage.memberScope.getContributedVariables(
            COROUTINE_SUSPENDED_NAME,
            NoLookupLocation.FROM_BACKEND
        ).filterNot { it.isExpect }.single().getter!!
    )

    val coroutineGetContext: IrSimpleFunctionSymbol
        get() {
            val contextGetter =
                continuationClass.owner.declarations.filterIsInstance<IrSimpleFunction>()
                    .atMostOne { it.name == CONTINUATION_CONTEXT_GETTER_NAME }
                    ?: continuationClass.owner.declarations.filterIsInstance<IrProperty>()
                        .atMostOne { it.name == CONTINUATION_CONTEXT_PROPERTY_NAME }?.getter!!
            return contextGetter.symbol
        }

    val coroutineContextProperty: PropertyDescriptor
        get() {
            val vars = coroutinePackage.memberScope.getContributedVariables(
                COROUTINE_CONTEXT_NAME,
                NoLookupLocation.FROM_BACKEND
            )
            return vars.single()
        }

    companion object {
        private val INTRINSICS_PACKAGE_NAME = Name.identifier("intrinsics")
        private val COROUTINE_SUSPENDED_NAME = Name.identifier("COROUTINE_SUSPENDED")
        private val COROUTINE_CONTEXT_NAME = Name.identifier("coroutineContext")
        private val COROUTINE_IMPL_NAME = Name.identifier("CoroutineImpl")
        private val CONTINUATION_NAME = Name.identifier("Continuation")
        private val CONTINUATION_CONTEXT_GETTER_NAME = Name.special("<get-context>")
        private val CONTINUATION_CONTEXT_PROPERTY_NAME = Name.identifier("context")
        private val COROUTINE_PACKAGE_FQNAME = FqName.fromSegments(listOf("kotlin", "coroutines"))
        private val COROUTINE_INTRINSICS_PACKAGE_FQNAME = COROUTINE_PACKAGE_FQNAME.child(INTRINSICS_PACKAGE_NAME)
    }
}

fun findClass(memberScope: MemberScope, name: Name): ClassDescriptor =
    memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor

fun findFunctions(memberScope: MemberScope, name: Name): List<SimpleFunctionDescriptor> =
    memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).toList()

interface JsCommonInlineClassesUtils : InlineClassesUtils {

    /**
     * Returns the inlined class for the given type, or `null` if the type is not inlined.
     */
    fun getInlinedClass(type: IrType): IrClass?

    fun isTypeInlined(type: IrType): Boolean {
        return getInlinedClass(type) != null
    }

    fun shouldValueParameterBeBoxed(parameter: IrValueParameter): Boolean {
        val function = parameter.parent as? IrSimpleFunction ?: return false
        val klass = function.parent as? IrClass ?: return false
        if (!isClassInlineLike(klass)) return false
        return parameter.isDispatchReceiver && function.isOverridableOrOverrides
    }

    /**
     * An intrinsic for creating an instance of an inline class from its underlying value.
     */
    val boxIntrinsic: IrSimpleFunctionSymbol

    /**
     * An intrinsic for obtaining the underlying value from an instance of an inline class.
     */
    val unboxIntrinsic: IrSimpleFunctionSymbol
}
