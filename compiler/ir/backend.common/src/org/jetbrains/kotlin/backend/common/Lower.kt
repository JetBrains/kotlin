/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.phaser.Action
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.util.transformSubsetFlat
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

interface FileLoweringPass {
    fun lower(irFile: IrFile)

    object Empty : FileLoweringPass {
        override fun lower(irFile: IrFile) {
            // Do nothing
        }
    }
}

interface ClassLoweringPass : FileLoweringPass {
    fun lower(irClass: IrClass)

    override fun lower(irFile: IrFile) = runOnFilePostfix(irFile)
}

interface ScriptLoweringPass : FileLoweringPass {
    fun lower(irScript: IrScript)

    override fun lower(irFile: IrFile) = runOnFilePostfix(irFile)
}

interface DeclarationContainerLoweringPass : FileLoweringPass {
    fun lower(irDeclarationContainer: IrDeclarationContainer)

    override fun lower(irFile: IrFile) = runOnFilePostfix(irFile)
}

interface BodyLoweringPass : FileLoweringPass {
    fun lower(irBody: IrBody, container: IrDeclaration)

    override fun lower(irFile: IrFile) = runOnFilePostfix(irFile)
}

interface BodyAndScriptBodyLoweringPass : BodyLoweringPass {
    fun lowerScriptBody(irDeclarationContainer: IrDeclarationContainer, container: IrDeclaration)

    override fun lower(irFile: IrFile) = runOnFilePostfix(irFile)
}

fun FileLoweringPass.lower(
    moduleFragment: IrModuleFragment
) = moduleFragment.files.forEach {
    try {
        lower(it)
    } catch (e: CompilationException) {
        e.file = it
        throw e
    } catch (e: Throwable) {
        throw e.wrapWithCompilationException(
            "Internal error in file lowering",
            it,
            null
        )
    }
}

fun ClassLoweringPass.runOnFilePostfix(irFile: IrFile) {
    irFile.acceptVoid(ClassLoweringVisitor(this))
}

private class ClassLoweringVisitor(
    private val loweringPass: ClassLoweringPass
) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        declaration.acceptChildrenVoid(this)
        loweringPass.lower(declaration)
    }
}

fun ScriptLoweringPass.runOnFilePostfix(irFile: IrFile) {
    irFile.acceptVoid(ScriptLoweringVisitor(this))
}

private class ScriptLoweringVisitor(
    private val loweringPass: ScriptLoweringPass
) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitScript(declaration: IrScript) {
        declaration.acceptChildrenVoid(this)
        loweringPass.lower(declaration)
    }
}

fun DeclarationContainerLoweringPass.runOnFilePostfix(irFile: IrFile) {
    irFile.acceptVoid(DeclarationContainerLoweringVisitor(this))
    lower(irFile as IrDeclarationContainer)
}

private class DeclarationContainerLoweringVisitor(
    private val loweringPass: DeclarationContainerLoweringPass
) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        declaration.acceptChildrenVoid(this)
        loweringPass.lower(declaration)
    }
}

fun BodyLoweringPass.runOnFilePostfix(
    irFile: IrFile,
    withLocalDeclarations: Boolean = false
) {
    val visitor = BodyLoweringVisitor(this, withLocalDeclarations)
    for (declaration in ArrayList(irFile.declarations)) {
        try {
            declaration.accept(visitor, null)
        } catch (e: CompilationException) {
            e.file = irFile
            throw e
        } catch (e: Throwable) {
            throw e.wrapWithCompilationException(
                "Internal error in body lowering",
                irFile,
                declaration
            )
        }
    }
}

fun BodyAndScriptBodyLoweringPass.runOnFilePostfix(irFile: IrFile) {
    val visitor = ScriptBodyLoweringVisitor(this)
    for (declaration in ArrayList(irFile.declarations)) {
        declaration.accept(visitor, null)
    }
}

private open class BodyLoweringVisitor(
    private val loweringPass: BodyLoweringPass,
    private val withLocalDeclarations: Boolean,
) : IrElementVisitor<Unit, IrDeclaration?> {
    override fun visitElement(element: IrElement, data: IrDeclaration?) {
        element.acceptChildren(this, data)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?) {
        declaration.acceptChildren(this, declaration)
    }

    override fun visitClass(declaration: IrClass, data: IrDeclaration?) {
        declaration.thisReceiver?.accept(this, declaration)
        declaration.typeParameters.forEach { it.accept(this, declaration) }
        ArrayList(declaration.declarations).forEach { it.accept(this, declaration) }
    }

    override fun visitBody(body: IrBody, data: IrDeclaration?) {
        if (withLocalDeclarations) body.acceptChildren(this, null)
        val stageController = data!!.factory.stageController
        stageController.restrictTo(data) {
            loweringPass.lower(body, data)
        }
    }

    override fun visitScript(declaration: IrScript, data: IrDeclaration?) {
        declaration.thisReceiver.accept(this, declaration)
        ArrayList(declaration.statements).forEach { it.accept(this, declaration) }
    }
}

private class ScriptBodyLoweringVisitor(
    private val loweringPass: BodyAndScriptBodyLoweringPass
) : BodyLoweringVisitor(loweringPass, false) {

    override fun visitClass(declaration: IrClass, data: IrDeclaration?) {
        declaration.thisReceiver?.accept(this, declaration)
        declaration.typeParameters.forEach { it.accept(this, declaration) }
        ArrayList(declaration.declarations).forEach { it.accept(this, declaration) }
        if (declaration.origin == IrDeclarationOrigin.SCRIPT_CLASS) {
            loweringPass.lowerScriptBody(declaration, declaration)
        }
    }
}

interface DeclarationTransformer : FileLoweringPass {
    fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>?

    val withLocalDeclarations: Boolean
        get() = false

    override fun lower(irFile: IrFile) {
        val visitor = Visitor(this)
        irFile.declarations.transformFlat { declaration ->
            try {
                declaration.acceptVoid(visitor)
                transformFlatRestricted(declaration)
            } catch (e: CompilationException) {
                e.file = irFile
                throw e
            } catch (e: Throwable) {
                throw e.wrapWithCompilationException(
                    "Internal error in declaration transformer",
                    irFile,
                    declaration
                )
            }
        }
    }

    private fun transformFlatRestricted(declaration: IrDeclaration): List<IrDeclaration>? {
        return declaration.factory.stageController.restrictTo(declaration) {
            transformFlat(declaration)
        }
    }

    private class Visitor(private val transformer: DeclarationTransformer) : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFunction(declaration: IrFunction) {
            declaration.acceptChildrenVoid(this)

            for (v in declaration.valueParameters) {
                val result = transformer.transformFlatRestricted(v)
                if (result != null) error("Don't know how to add value parameters")
            }
        }

        override fun visitProperty(declaration: IrProperty) {
            // TODO This is a hack to allow lowering a getter separately from the enclosing property

            val visitor = this

            fun IrDeclaration.replaceInContainer(container: MutableList<in IrDeclaration>, result: List<IrDeclaration>): Boolean {
                var index = container.indexOf(this)
                if (index == -1) {
                    index = container.indexOf(declaration)
                } else {
                    container.removeAt(index)
                    --index
                }
                return container.addAll(index + 1, result)
            }

            fun IrDeclaration.transform() {

                acceptVoid(visitor)

                val result = transformer.transformFlatRestricted(this)
                if (result != null) {
                    when (val parentCopy = parent) {
                        is IrDeclarationContainer -> replaceInContainer(parentCopy.declarations, result)
                        is IrStatementContainer -> replaceInContainer(parentCopy.statements, result)
                    }
                }
            }

            declaration.backingField?.transform()
            declaration.getter?.transform()
            declaration.setter?.transform()
        }

        override fun visitClass(declaration: IrClass) {
            declaration.thisReceiver?.accept(this, null)
            declaration.typeParameters.forEach { it.accept(this, null) }
            ArrayList(declaration.declarations).forEach { it.accept(this, null) }

            declaration.declarations.transformFlat {
                transformer.transformFlatRestricted(it)
            }
        }

        override fun visitScript(declaration: IrScript) {
            ArrayList(declaration.statements).forEach {
                if (transformer.withLocalDeclarations || it is IrDeclaration) {
                    it.accept(this, null)
                }
            }
            declaration.statements.transformSubsetFlat(transformer::transformFlatRestricted)

            declaration.thisReceiver.accept(this, null)
        }
    }
}

fun <C> Action<IrElement, C>.toMultiModuleAction(): Action<Iterable<IrModuleFragment>, C> {
    return { state, modules, context ->
        modules.forEach { module ->
            this(state, module, context)
        }
    }
}
