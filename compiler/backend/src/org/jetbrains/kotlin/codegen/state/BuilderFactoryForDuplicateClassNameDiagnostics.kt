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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.jvm
import org.jetbrains.kotlin.resolve.jvm.diagnostics.*
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*


class BuilderFactoryForDuplicateClassNameDiagnostics(
        builderFactory: ClassBuilderFactory,
        private val diagnostics: DiagnosticSink
) : ClassNameCollectionClassBuilderFactory(builderFactory) {

    private val className = hashMapOf<String, JvmDeclarationOrigin> ()

    override fun handleClashingNames(internalName: String, origin: JvmDeclarationOrigin) {
        val another = className.getOrPut(internalName, { origin })
        if (origin.element != another.element) {
            if (origin.originKind == JvmDeclarationOriginKind.PACKAGE_FACADE || another.originKind == JvmDeclarationOriginKind.PACKAGE_FACADE) {
                if (origin.originKind == JvmDeclarationOriginKind.PACKAGE_FACADE) {
                    reportError(another, internalName)
                } else {
                    reportError(another, internalName)
                }
            } else {
                if (origin.element != null) {
                    reportError(origin, internalName)
                }
                if (another.element != null) {
                    reportError(another, internalName)
                }
            }
        }
    }

    private fun reportError(another: JvmDeclarationOrigin, internalName: String) {
        diagnostics.report(ErrorsJvm.DUPLICATE_CLASS_NAMES.on(another.element, internalName))
    }
}