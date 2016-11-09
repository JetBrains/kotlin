/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.renderer.DescriptorRenderer

fun <P : Any> renderParameter(parameter: P, renderer: DiagnosticParameterRenderer<P>?, context: RenderingContext): Any
        = renderer?.render(parameter, context) ?: parameter

fun ClassifierDescriptorWithTypeParameters.renderKindWithName(): String = DescriptorRenderer.getClassifierKindPrefix(this) + " '" + name + "'"

fun ClassDescriptor.renderKind(): String = DescriptorRenderer.getClassifierKindPrefix(this)

