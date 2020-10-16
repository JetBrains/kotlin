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

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.util.transformFlat
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

interface FunctionLoweringPass : FileLoweringPass {
    fun lower(irFunction: IrFunction)

    override fun lower(irFile: IrFile) = runOnFilePostfix(irFile)
}

interface BodyLoweringPass : FileLoweringPass {
    fun lower(irBody: IrBody, container: IrDeclaration)

    override fun lower(irFile: IrFile) = runOnFilePostfix(irFile)
}

fun FileLoweringPass.lower(moduleFragment: IrModuleFragment) = moduleFragment.files.forEach { lower(it) }

//fun FileLoweringPass.lower(modules: Iterable<IrModuleFragment>) {
//    modules.forEach { module ->
//        module.files.forEach {
//            lower(it)
//        }
//    }
//}

fun ClassLoweringPass.runOnFilePostfix(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            declaration.acceptChildrenVoid(this)
            lower(declaration)
        }
    })
}

fun ScriptLoweringPass.runOnFilePostfix(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitScript(declaration: IrScript) {
            declaration.acceptChildrenVoid(this)
            lower(declaration)
        }
    })
}

fun DeclarationContainerLoweringPass.asClassLoweringPass() = object : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        this@asClassLoweringPass.lower(irClass)
    }
}

fun DeclarationContainerLoweringPass.asScriptLoweringPass() = object : ScriptLoweringPass {
    override fun lower(irScript: IrScript) {
        this@asScriptLoweringPass.lower(irScript)
    }
}

fun DeclarationContainerLoweringPass.runOnFilePostfix(irFile: IrFile) {
    this.asClassLoweringPass().runOnFilePostfix(irFile)
    this.asScriptLoweringPass().runOnFilePostfix(irFile)

    this.lower(irFile as IrDeclarationContainer)
}

fun BodyLoweringPass.runOnFilePostfix(
    irFile: IrFile,
    withLocalDeclarations: Boolean = false,
    allowDeclarationModification: Boolean = false
) {
    ArrayList(irFile.declarations).forEach {
        it.accept(object : IrElementVisitor<Unit, IrDeclaration?> {
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
                if (allowDeclarationModification) {
                    lower(body, data!!)
                } else {
                    stageController.bodyLowering {
                        lower(body, data!!)
                    }
                }
            }

            override fun visitScript(declaration: IrScript, data: IrDeclaration?) {
                ArrayList(declaration.declarations).forEach { it.accept(this, declaration) }
                if (withLocalDeclarations) {
                    declaration.statements.forEach { it.accept(this, null) }
                }
                declaration.thisReceiver.accept(this, declaration)
            }

        }, null)
    }
}

fun FunctionLoweringPass.runOnFilePostfix(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFunction(declaration: IrFunction) {
            declaration.acceptChildrenVoid(this)
            lower(declaration)
        }
    })
}

interface DeclarationTransformer : FileLoweringPass {
    fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>?

    override fun lower(irFile: IrFile) {
        runPostfix().toFileLoweringPass().lower(irFile)
    }
}

fun DeclarationTransformer.transformFlatRestricted(declaration: IrDeclaration): List<IrDeclaration>? {
    return stageController.restrictTo(declaration) {
        transformFlat(declaration)
    }
}

fun DeclarationTransformer.toFileLoweringPass(): FileLoweringPass {
    return object : FileLoweringPass {
        override fun lower(irFile: IrFile) {
            irFile.declarations.transformFlat(this@toFileLoweringPass::transformFlat)
        }
    }
}

fun DeclarationTransformer.runPostfix(withLocalDeclarations: Boolean = false): DeclarationTransformer {
    return object : DeclarationTransformer {
        override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
            declaration.acceptVoid(PostfixDeclarationTransformer(withLocalDeclarations, this@runPostfix))

            return this@runPostfix.transformFlatRestricted(declaration)
        }
    }
}

private class PostfixDeclarationTransformer(
    private val withLocalDeclarations: Boolean,
    private val transformer: DeclarationTransformer
) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitBody(body: IrBody) {
        if (withLocalDeclarations) {
            super.visitBody(body)
        }
        // else stop
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

        fun IrDeclaration.transform() {

            acceptVoid(visitor)

            val result = transformer.transformFlatRestricted(this)
            if (result != null) {
                (parent as? IrDeclarationContainer)?.let {
                    var index = it.declarations.indexOf(this)
                    if (index == -1) {
                        index = it.declarations.indexOf(declaration)
                    } else {
                        it.declarations.removeAt(index)
                        --index
                    }

                    it.declarations.addAll(index + 1, result)
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

        declaration.declarations.transformFlat(transformer::transformFlatRestricted)
    }

    override fun visitScript(declaration: IrScript) {
        ArrayList(declaration.declarations).forEach { it.accept(this, null) }
        declaration.declarations.transformFlat(transformer::transformFlatRestricted)

        if (withLocalDeclarations) {
            declaration.statements.forEach { it.accept(this, null) }
        }

        declaration.thisReceiver.accept(this, null)
    }
}