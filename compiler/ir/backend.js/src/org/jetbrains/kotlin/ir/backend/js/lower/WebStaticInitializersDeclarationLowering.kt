/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.correspondingField
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

/**
 * Moves initializers of static members of the class coming from companion blocks into a static initializer function.
 *
 * Before:
 * ```kotlin
 * class Foo {
 *   companion {
 *     val first = initFirst()
 *   }
 *   companion object {
 *     val second = initSecond()
 *   }
 *   companion {
 *     val third = initThird()
 *   }
 * }
 * ```
 *
 * After:
 * ```kotlin
 * class Foo {
 *   companion {
 *     var static_init_called = 0
 *     static_init() {
 *       if (checkInitializationState(static_init_called, Foo::class)) return
 *       static_init_called = 1
 *       try {
 *         first = initFirst()
 *         second = initSecond()
 *         third = initThird()
 *       } catch (reason: Throwable) {
 *         static_init_called = 2
 *         kotlint.internal.staticInitializationFailure(reason, null)
 *       }
 *     }
 *   }
 *   companion {
 *     val first: FirstType
 *   }
 *   companion object {
 *     val second: SecondType
 *   }
 *   companion {
 *     val third: ThirdType
 *   }
 * }
 * ```
 */
abstract class WebStaticInitializersDeclarationLowering : FileLoweringPass {
    companion object {
        val STATIC_FIELD_INITIALIZER by IrStatementOriginImpl
        val STATIC_CLASS_INITIALIZER by IrDeclarationOriginImpl.Synthetic

        const val STATIC_INIT_FUNCTION_NAME = "static_init"
        const val STATIC_INIT_CALLED_PROPERTY_NAME = "static_init_called"
    }

    protected abstract val context: JsCommonBackendContext

    protected abstract val initializationGenerator: LazyGlobalInitializationGenerator

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitFile(declaration: IrFile) {
                declaration.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                processDeclarationContainer(declaration)
                declaration.acceptChildrenVoid(this)
            }
        })
    }

    private fun processDeclarationContainer(container: IrClass) {
        if (container.isEffectivelyExternal()) return

        // A class may already have been processed by a lowering instance bound to another module, see the comment below.
        if (container.staticInitializerProcessed) return
        container.staticInitializerProcessed = true

        // Before building child static_init, we need to be sure that all super types are processed in order before the child.
        // This is needed for 2 reasons:
        // 1. To create a call to a parent static_init in the child static_init body.
        // 2. To create child static_init even if the child doesn't have any initializers, but super class has.
        var hasSuperTypeWithStaticInitializer = false
        val dependencySuperTypes = container.dependencySuperTypes
        dependencySuperTypes.forEach {
            processDeclarationContainer(it)
            if (it.staticInitFunction != null) hasSuperTypeWithStaticInitializer = true
        }
        val needsStaticInitFunction = container.declarations.any {
            when (it) {
                is IrEnumEntry -> it.correspondingField?.isStatic == true
                is IrField -> it.isStatic && it.origin != IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE &&
                        it.correspondingPropertySymbol?.owner?.isLateinit == false
                is IrProperty -> it.backingField?.isStatic == true && !it.isLateinit
                // A companion object is initialized together with its container, so the container needs
                // a static_init as soon as the companion has anything observable to initialize. Otherwise, we omit it to
                // not blow up the bundle size.
                is IrClass if it.isCompanion && !it.isInitializersFreeClass() -> true
                else -> false
            }
        }

        if (!needsStaticInitFunction && !hasSuperTypeWithStaticInitializer) return

        val initializers = buildList {
            for (declaration in container.declarations) {
                // Special handling of companion objects - if the static_init function is introduced, the Companion$getInstance
                // body should be moved to the static_init body to preserve the correct order of initialization.
                // $getInstance then calls static_init instead.
                if (declaration is IrClass && declaration.isCompanion) {
                    declaration.objectGetInstanceFunction?.let { getInstance ->
                        val body = getInstance.body as? IrBlockBody ?: return@let
                        body.statements.let { statements ->
                            // Relying on the fact that $getInstance always ends with IrReturn
                            addAll(statements.dropLast(1))
                            val irReturn = statements.last()
                            statements.clear()
                            statements.add(irReturn)
                        }
                    }
                    continue
                }

                val [field, initializerBody] = when (declaration) {
                    is IrEnumEntry -> declaration.correspondingField to declaration.initializerExpression
                    is IrField -> declaration to declaration.initializer
                    is IrProperty -> declaration.backingField to declaration.backingField?.initializer
                    else -> continue
                }
                if (field == null || initializerBody == null) continue
                if (!field.isStatic) continue

                add(container.createInitializer(declaration, field, initializerBody.expression))
                field.initializer = null
            }
        }

        // It is important to define stable signature via restrictTo to be able to reference static_init of super class
        // defined in a separate module.
        //
        // Both declarations must be created within the *same* restrictTo block: the stage controller resets its signature index
        // on every restrictTo call, so creating them in two separate blocks gives both the very same
        // `IdSignature.LoweredDeclarationSignature`. Cross-file references are resolved by the rendered signature, so the
        // collision makes a reference to `static_init` resolve to `static_init_called` instead.
        val [staticInitCalledField, staticInitFunction] = context.irFactory.stageController.restrictTo(container) {
            val stateField = initializationGenerator.createStateField(
                name = Name.identifier(STATIC_INIT_CALLED_PROPERTY_NAME),
                origin = STATIC_CLASS_INITIALIZER,
            ).apply {
                parent = container
            }
            val initFunction = initializationGenerator.createStaticInitFunction(
                name = Name.identifier(STATIC_INIT_FUNCTION_NAME),
                klass = container,
                origin = STATIC_CLASS_INITIALIZER,
                stateField = stateField,
                initializers = initializers,
                visibility = DescriptorVisibilities.PUBLIC,
            ) {
                val [dependencySuperInterfaces, dependencySuperClasses] =
                    dependencySuperTypes.partition { it.isInterface }
                for (superClass in dependencySuperClasses + dependencySuperInterfaces) {
                    superClass.staticInitFunction?.let {
                        +irCall(it.symbol)
                    }
                }
            }.apply {
                parent = container
            }
            stateField to initFunction
        }

        // Adding static_init declaration after adding its usages to make sure we don't insert usages inside static_init itself
        container.staticInitFunction = staticInitFunction
        container.companionObject()?.staticInitFunction = staticInitFunction
        container.declarations.addAll(0, listOf(staticInitCalledField, staticInitFunction))
    }

    private fun IrClass.createInitializer(declaration: IrDeclaration, field: IrField, initializer: IrExpression): IrSetField =
        context.irBuiltIns.createIrBuilder(symbol, declaration.startOffset, declaration.endOffset).run {
            irSetField(
                receiver = null,
                field = field,
                value = initializer,
                origin = STATIC_FIELD_INITIALIZER
            )
    }

    private inline fun IrClass.traverseSuperTypes(f: (IrClass) -> Unit) {
        for (superType in superTypes) {
            if (superType.isAny()) continue
            val superClass = superType.classOrNull?.owner ?: continue
            f(superClass)
        }
    }

    // In the case of super interfaces, only ones having at least 1 non-abstract, non-static member
    // trigger its initialization from the implementing class. The ordering follows the recursive
    // enumeration over the superinterface hierarchy of each directly implemented interface. See
    // section §3.3 of the KEEP and JVM spec section §5.5 step 7. Here, the behavior is aligned with
    // what is done on the JVM.
    private val IrClass.dependencySuperTypes: List<IrClass>
        get() = collectSuperDependencies(this)

    private fun collectSuperDependencies(clazz: IrClass): List<IrClass> {
        val result = mutableListOf<IrClass>()
        val visited = mutableSetOf<IrClass>()
        clazz.traverseSuperTypes {
            when {
                it.isInterface -> collectInterfaceDependencies(it, result, visited)
                visited.add(it) -> result.add(it)
            }
        }
        return result
    }

    private fun collectInterfaceDependencies(iface: IrClass, result: MutableList<IrClass>, visited: MutableSet<IrClass>) {
        if (!visited.add(iface)) return

        iface.traverseSuperTypes {
            if (it.isInterface) {
                collectInterfaceDependencies(it, result, visited)
            }
        }

        if (iface.declarations.any { it.isNonAbstractInstanceMember() }) {
            result.add(iface)
        }
    }

    private fun IrDeclaration.isNonAbstractInstanceMember(): Boolean = when (this) {
        is IrSimpleFunction if isReal && modality != Modality.ABSTRACT && dispatchReceiverParameter != null -> true
        is IrProperty if isReal && modality != Modality.ABSTRACT && (getter ?: setter)?.dispatchReceiverParameter != null -> true
        else -> false // nested classes, companion object, fields, etc. don't count
    }

    private fun IrClass.isInitializersFreeClass(): Boolean {
        return when {
            superTypes.any { !it.isAny() } -> false
            declarations.any { it is IrField } -> false
            declarations.any { it is IrAnonymousInitializer } -> false
            else -> true
        }
    }
}
