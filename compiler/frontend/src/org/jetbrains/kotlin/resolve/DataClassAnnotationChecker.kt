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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.builtins.*


public class DataClassAnnotationChecker : AnnotationChecker {
    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor !is ClassDescriptor) return
        if (declaration !is JetClassOrObject) return

        if (KotlinBuiltIns.isData(descriptor)) {
            if (descriptor.getUnsubstitutedPrimaryConstructor() == null && descriptor.getConstructors().isNotEmpty()) {
                diagnosticHolder.report(Errors.PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS.on(declaration.getNameIdentifier()));
            }
        }
    }
}
