/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.backend.common.lower.LoweredStatementOrigins.INLINED_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.backend.common.lower.LoweredStatementOrigins.INLINED_FUNCTION_DEFAULT_ARGUMENTS
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.GlobalBirDynamicProperties
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.get
import org.jetbrains.kotlin.bir.getOrPutDynamicProperty
import org.jetbrains.kotlin.bir.util.isAnonymousObject
import org.jetbrains.kotlin.bir.util.isFunctionInlining
import org.jetbrains.kotlin.bir.util.name
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

context(BirBackendContext)
abstract class BirInventNamesForLocalClassesLowering(
    private val allowTopLevelCallables: Boolean,
    private val generateNamesForRegeneratedObjects: Boolean = false,
) : BirLoweringPhase() {
    private val localNameScopeKey = createLocalIrProperty<_, LocalNameScope>(BirElement)

    private val anonymousClassesCount = mutableMapOf<String, Int>()

    private data class LocalNameScope(
        val enclosingName: String?,
        val isLocal: Boolean,
        val processingInlinedFunction: Boolean = false,
        val skipNaming: Boolean = false,
    ) {
        fun makeLocal() = if (isLocal) this else copy(isLocal = true)
    }

    override fun lower(module: BirModuleFragment) {
        getAllElementsOfClass(BirClass, false).forEach { clazz ->
            val nameScope = getLocalNameScopeIfApplicable(clazz) ?: return@forEach
            if (nameScope.isLocal) {
                putLocalClassName(clazz, nameScope.enclosingName!!)
            }
        }

        getAllElementsOfClass(BirFunctionExpression, false).forEach { expression ->
            val internalName = (if (isLocalFunctionToBeNamed(expression.function!!))
                getLocalNameScopeIfApplicable(expression.function)?.enclosingName
            else null)
                ?: inventName(null, getLocalNameScopeIfApplicable(expression) ?: return@forEach)

            putLocalClassName(expression, internalName)
        }

        getAllElementsOfClass(BirFunctionReference, false).forEach { reference ->
            val localNameScope = getLocalNameScopeIfApplicable(reference) ?: return@forEach
            if (localNameScope.processingInlinedFunction && reference[GlobalBirDynamicProperties.OriginalBeforeInline] == null) {
                // skip BirFunctionReference from `singleArgumentInlineFunction`
                return@forEach
            }

            val function = reference.symbol.owner
            val internalName = (if (isLocalFunctionToBeNamed(function))
                getLocalNameScopeIfApplicable(function)?.enclosingName
            else null)
                ?: inventName(null, localNameScope)

            putLocalClassName(reference, internalName)
        }

        getAllElementsOfClass(BirPropertyReference, false).forEach { property ->
            val nameScope = getLocalNameScopeIfApplicable(property) ?: return@forEach
            val internalName = inventName(null, nameScope)
            putLocalClassName(property, internalName)
        }

        getAllElementsOfClass(BirSimpleFunction, false).forEach { function ->
            // Suspend functions have a continuation, which is essentially a local class
            if (function.isSuspend && function.correspondingPropertySymbol == null && function.body != null && function.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
                val nameScope = getLocalNameScopeIfApplicable(function) ?: return@forEach
                val internalName = inventName(function.name, nameScope)
                putLocalClassName(function, internalName)
            }
        }
    }

    private fun getLocalNameScopeIfApplicable(element: BirElement?): LocalNameScope? =
        getLocalNameScope(element).takeUnless { it.skipNaming }

    private fun getLocalNameScope(element: BirElement?): LocalNameScope {
        if (element == null) {
            return LocalNameScope(null, false)
        }
        return element.getOrPutDynamicProperty(localNameScopeKey) {
            computeLocalNameScope(element)
        }
    }

    private fun computeLocalNameScope(element: BirElement): LocalNameScope {
        val parent = element.parent
        val parentContext = getLocalNameScope(parent)

        if (parentContext.skipNaming)
            return parentContext

        if (parent is BirInlinedFunctionBlock) {
            if (!generateNamesForRegeneratedObjects && !(element is BirComposite && element.origin == INLINED_FUNCTION_ARGUMENTS)) {
                return parentContext.copy(skipNaming = true)
            }
            if (!parentContext.processingInlinedFunction && parent.isFunctionInlining()) {
                if (element is BirComposite && (element.origin == INLINED_FUNCTION_ARGUMENTS || element.origin == INLINED_FUNCTION_DEFAULT_ARGUMENTS)) {
                    return parentContext
                } else {
                    val inlinedAt = parent.inlineCall.symbol.owner.name.asString()
                    return parentContext.copy(
                        enclosingName = parentContext.enclosingName + "$\$inlined\$$inlinedAt",
                        isLocal = true,
                        processingInlinedFunction = true
                    )
                }
            }
        }

        return when (element) {
            is BirConstructor -> {
                // Old backend doesn't add the anonymous object name to the stack when traversing its super constructor arguments.
                // E.g. a lambda in the super call of an object literal "foo$1" will get the name "foo$2", not "foo$1$1".
                val inheritedContext =
                    if (parent is BirClass && !parent.isAnonymousObject && parentContext.isLocal)
                        getLocalNameScope(parent)
                    else parentContext

                // Constructor is a special case because its name "<init>" doesn't participate when creating names for local classes inside.
                inheritedContext.makeLocal()
            }
            is BirEnumEntry -> {
                // Although IrEnumEntry is an IrDeclaration, its name shouldn't be added to nameStack. This is because each IrEnumEntry has
                // an IrClass with the same name underneath it, and that class should obtain the name of the form "Enum$Entry",
                // not "Enum$Entry$Entry".
                parentContext.makeLocal()
            }
            is BirValueParameter -> {
                // We skip value parameters when constructing names to replicate behavior of the old backend, but this can be safely changed.
                parentContext.makeLocal()
            }
            is BirField -> {
                // Skip field name because the name of the property is already there.
                parentContext.makeLocal()
            }
            is BirAnonymousInitializer -> {
                // BirAnonymousInitializer is not an BirDeclaration, so we need to manually make all its children aware that they're local
                // and might need new invented names.
                parentContext.makeLocal()
            }
            is BirClass -> {
                val isTopLevelAnonymous = parent is BirFile && element.isAnonymousObject
                val isLocal = parentContext.isLocal || isTopLevelAnonymous
                val enclosingName = if (isTopLevelAnonymous) {
                    (parent as BirFile).name.removeSuffix(".kt").plus("Kt").capitalizeAsciiOnly()
                } else parentContext.enclosingName

                val internalName = if (isLocal) {
                    inventName(element.name, parentContext, enclosingName)
                } else {
                    // This is not a local class, so we need not invent a name for it, the type mapper will correctly compute it
                    // by navigating through its containers.
                    if (enclosingName != null) {
                        "$enclosingName$${element.name.asString()}"
                    } else {
                        computeTopLevelClassName(element)
                    }
                }

                parentContext.copy(enclosingName = internalName, isLocal = isLocal)
            }
            is BirDeclarationWithName -> {
                if (
                // Skip temporary variables because they are not present in source code, and their names are not particularly
                // meaningful (e.g. `tmp$1`) in any case.
                    element.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR ||
                    element.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE ||
                    // Skip variables storing delegates for local properties because we already have the name of the property itself.
                    element.origin == IrDeclarationOrigin.PROPERTY_DELEGATE
                ) return parentContext

                // Skip adding property accessors to the name stack because the name of the property (which is a parent) is already there.
                if (element is BirSimpleFunction && element.correspondingPropertySymbol != null)
                    return parentContext.makeLocal()

                val simpleName = element.name.asString()
                val enclosingName = parentContext.enclosingName
                val internalName = when {
                    // Replace "unnamed" function names with indices.
                    element is BirFunction && isLocalFunctionToBeNamed(element) -> {
                        if (parent == null) {
                            return parentContext.copy(skipNaming = true)
                        }
                        inventName(null, parentContext)
                    }
                    element is BirVariable && generateNamesForRegeneratedObjects || parentContext.processingInlinedFunction -> enclosingName
                    enclosingName != null -> "$enclosingName$$simpleName"
                    else -> simpleName
                }

                if ((element is BirProperty && element.isDelegated) || element is BirLocalDelegatedProperty) {
                    // Old backend currently reserves a name here, in case a property reference-like anonymous object will need
                    // to be generated in the codegen later, which is now happening for local delegated properties in inline functions.
                    // See CodegenAnnotatingVisitor.visitProperty and ExpressionCodegen.initializePropertyMetadata.
                    inventName(null, parentContext, internalName)
                }

                parentContext.copy(enclosingName = internalName, isLocal = true)
            }
            else -> parentContext
        }
    }

    private fun isLocalFunctionToBeNamed(element: BirDeclarationWithName) = !NameUtils.hasName(element.name)


    private fun inventName(sourceName: Name?, nameScope: LocalNameScope, enclosingName: String? = nameScope.enclosingName): String {
        check(enclosingName != null) {
            """
                    There should be at least one name in the stack for every local declaration that needs a name
                    Source name: $sourceName
                    Local name context: $nameScope 
                """.trimIndent()
        }

        val simpleName = if (sourceName == null || sourceName.isSpecial) {
            val count = anonymousClassesCount.compute(enclosingName.toUpperCaseAsciiOnly()) { _, v -> (v ?: 0) + 1 }
            count.toString()
        } else {
            sourceName.asString()
        }

        return sanitizeNameIfNeeded("$enclosingName$$simpleName")
    }

    protected abstract fun computeTopLevelClassName(clazz: BirClass): String
    protected abstract fun sanitizeNameIfNeeded(name: String): String
    protected abstract fun putLocalClassName(declaration: BirAttributeContainer, localClassName: String)
}