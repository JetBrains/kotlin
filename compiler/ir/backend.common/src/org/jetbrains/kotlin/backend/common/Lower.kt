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

import org.jetbrains.kotlin.backend.common.DeclarationTransformer.TransformingVisitor
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
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

interface ModuleLoweringPass {
    fun lower(irModule: IrModuleFragment)
}

interface FileLoweringPass : ModuleLoweringPass {
    fun lower(irFile: IrFile)

    override fun lower(irModule: IrModuleFragment) {
        for (file in irModule.files) {
            try {
                lower(file)
            } catch (e: CompilationException) {
                e.initializeFileDetails(file)
                throw e
            } catch (e: KotlinExceptionWithAttachments) {
                throw e
            } catch (e: Throwable) {
                throw e.wrapWithCompilationException("Internal error in file lowering", file, null)
            }
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
            e.initializeFileDetails(irFile)
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
        declaration.thisReceiver?.accept(this, declaration)
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
                declaration.accept(visitor, declaration)
                transformFlatRestricted(declaration)
            } catch (e: CompilationException) {
                e.initializeFileDetails(irFile)
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

    fun transformFlatRestricted(declaration: IrDeclaration): List<IrDeclaration>? {
        return declaration.factory.stageController.restrictTo(declaration) {
            transformFlat(declaration)
        }
    }

    private class Visitor(override val transformer: DeclarationTransformer) : TransformingVisitor {
        override fun visitElement(element: IrElement, data: IrDeclaration?) {
            element.acceptChildren(this, data)
        }
    }

    interface TransformingVisitor : IrElementVisitor<Unit, IrDeclaration?> {
        val transformer: DeclarationTransformer

        override fun visitFunction(declaration: IrFunction, data: IrDeclaration?) {
            declaration.acceptChildren(this, declaration)

            for (v in declaration.valueParameters) {
                val result = transformer.transformFlatRestricted(v)
                if (result != null) error("Don't know how to add value parameters")
            }
        }

        override fun visitProperty(declaration: IrProperty, data: IrDeclaration?) {
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

                accept(visitor, data)

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

        override fun visitClass(declaration: IrClass, data: IrDeclaration?) {
            declaration.thisReceiver?.accept(this, declaration)
            declaration.typeParameters.forEach { it.accept(this, declaration) }
            ArrayList(declaration.declarations).forEach { it.accept(this, declaration) }

            declaration.declarations.transformFlat {
                transformer.transformFlatRestricted(it)
            }
        }

        override fun visitScript(declaration: IrScript, data: IrDeclaration?) {
            ArrayList(declaration.statements).forEach {
                if (transformer.withLocalDeclarations || it is IrDeclaration) {
                    it.accept(this, declaration)
                }
            }
            declaration.statements.transformSubsetFlat(transformer::transformFlatRestricted)

            declaration.thisReceiver?.accept(this, declaration)
        }
    }
}

abstract class BodyLoweringDeclarationTransformerPass(private val transformer: DeclarationTransformer) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        val visitor = BodyLoweringDeclarationTransformerVisitor(transformer, this)
        for (declaration in ArrayList(irFile.declarations)) {
            try {
                declaration.accept(visitor, null)
                transformer.transformFlatRestricted(declaration)
            } catch (e: CompilationException) {
                e.initializeFileDetails(irFile)
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

    private class BodyLoweringDeclarationTransformerVisitor(
        override val transformer: DeclarationTransformer,
        loweringPass: BodyLoweringDeclarationTransformerPass,
    ) : BodyLoweringVisitor(loweringPass, transformer.withLocalDeclarations), TransformingVisitor {
        override fun visitFunction(declaration: IrFunction, data: IrDeclaration?) = super<TransformingVisitor>.visitFunction(declaration, data)
        override fun visitProperty(declaration: IrProperty, data: IrDeclaration?) = super<TransformingVisitor>.visitProperty(declaration, data)
        override fun visitClass(declaration: IrClass, data: IrDeclaration?) = super<TransformingVisitor>.visitClass(declaration, data)
        override fun visitScript(declaration: IrScript, data: IrDeclaration?) = super<TransformingVisitor>.visitScript(declaration, data)
    }
}

fun <C> Action<IrElement, C>.toMultiModuleAction(): Action<Iterable<IrModuleFragment>, C> {
    return { state, modules, context ->
        modules.forEach { module ->
            this(state, module, context)
        }
    }
}
