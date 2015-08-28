/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.state

import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.ClassNameCollectionClassBuilderFactory
import org.jetbrains.kotlin.codegen.SignatureCollectingClassBuilderFactory
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.load.java.descriptors.SamAdapterDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.jvm
import org.jetbrains.kotlin.resolve.jvm.diagnostics.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.join
import java.util.*


class BuilderFactoryForDuplicateClassNameDiagnostics(
        builderFactory: ClassBuilderFactory,
        private val diagnostics: DiagnosticSink
) : ClassNameCollectionClassBuilderFactory(builderFactory) {

    private val className = hashMapOf<String, JvmDeclarationOrigin> ()

    override fun handleClashingNames(internalName: String, origin: JvmDeclarationOrigin) {
        val another = className.getOrPut(internalName, { origin })
        //workaround for inlined anonymous objects
        if (origin.element != another.element) {
            reportError(internalName, origin, another)
        }
    }

    private fun reportError(internalName: String, vararg another: JvmDeclarationOrigin) {
        val fromString = another.map { it.descriptor }.filterNotNull().
                joinToString { DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(it) }

        another.map { it.element }.filterNotNull().forEach {
            diagnostics.report(ErrorsJvm.DUPLICATE_CLASS_NAMES.on(it, internalName, fromString))
        }
    }
}