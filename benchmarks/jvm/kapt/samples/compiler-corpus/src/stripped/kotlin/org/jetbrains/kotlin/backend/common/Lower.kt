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

import org.jetbrains.kotlin.config.phaser.Action
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.util.transformSubsetFlat
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

interface ModuleLoweringPass {
    fun lower(irModule: IrModuleFragment)
}

interface FileLoweringPass : ModuleLoweringPass {
    fun lower(irFile: IrFile)

    override fun lower(irModule: IrModuleFragment) { TODO() }
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

fun ClassLoweringPass.runOnFilePostfix(irFile: IrFile) { TODO() }

private class ClassLoweringVisitor(
    private val loweringPass: ClassLoweringPass
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) { TODO() }

    override fun visitClass(declaration: IrClass) { TODO() }
}

fun ScriptLoweringPass.runOnFilePostfix(irFile: IrFile) { TODO() }

private class ScriptLoweringVisitor(
    private val loweringPass: ScriptLoweringPass
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) { TODO() }

    override fun visitScript(declaration: IrScript) { TODO() }
}

fun DeclarationContainerLoweringPass.runOnFilePostfix(irFile: IrFile) { TODO() }

private class DeclarationContainerLoweringVisitor(
    private val loweringPass: DeclarationContainerLoweringPass
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) { TODO() }

    override fun visitClass(declaration: IrClass) { TODO() }
}

fun BodyLoweringPass.runOnFilePostfix(
    irFile: IrFile,
    withLocalDeclarations: Boolean = false
) { TODO() }

fun BodyAndScriptBodyLoweringPass.runOnFilePostfix(irFile: IrFile) { TODO() }

private open class BodyLoweringVisitor(
    private val loweringPass: BodyLoweringPass,
    private val withLocalDeclarations: Boolean,
) : IrVisitor<Unit, IrDeclaration?>() {
    override fun visitElement(element: IrElement, data: IrDeclaration?) { TODO() }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?) { TODO() }

    override fun visitClass(declaration: IrClass, data: IrDeclaration?) { TODO() }

    override fun visitBody(body: IrBody, data: IrDeclaration?) { TODO() }

    override fun visitScript(declaration: IrScript, data: IrDeclaration?) { TODO() }
}

private class ScriptBodyLoweringVisitor(
    private val loweringPass: BodyAndScriptBodyLoweringPass
) : BodyLoweringVisitor(loweringPass, false) {

    override fun visitClass(declaration: IrClass, data: IrDeclaration?) { TODO() }
}

interface DeclarationTransformer : FileLoweringPass {
    fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>?

    val withLocalDeclarations: Boolean
        get() = TODO()

    override fun lower(irFile: IrFile) { TODO() }

    private fun transformFlatRestricted(declaration: IrDeclaration): List<IrDeclaration>? { TODO() }

    private class Visitor(private val transformer: DeclarationTransformer) : IrVisitorVoid() {
        override fun visitElement(element: IrElement) { TODO() }

        override fun visitFunction(declaration: IrFunction) { TODO() }

        override fun visitProperty(declaration: IrProperty) { TODO() }

        override fun visitClass(declaration: IrClass) { TODO() }

        override fun visitScript(declaration: IrScript) { TODO() }
    }
}

fun <C> Action<IrElement, C>.toMultiModuleAction(): Action<Iterable<IrModuleFragment>, C> { TODO() }
