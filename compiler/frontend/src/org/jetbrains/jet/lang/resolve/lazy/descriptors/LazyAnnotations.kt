/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.resolve.lazy.LazyEntity
import org.jetbrains.jet.lang.psi.JetAnnotationEntry
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil
import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.lang.resolve.AnnotationResolver
import org.jetbrains.jet.lang.resolve.BindingTrace
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.utils.keysToMapExceptNulls
import org.jetbrains.jet.lang.resolve.BindingContext

abstract class LazyAnnotationsContext(
         val annotationResolver: AnnotationResolver,
         val storageManager: StorageManager,
         val trace: BindingTrace
) {
    abstract val scope: JetScope
}

public class LazyAnnotations(
        val c: LazyAnnotationsContext,
        val annotationEntries: List<JetAnnotationEntry>
) : Annotations, LazyEntity {
    override fun isEmpty() = annotationEntries.isEmpty()

    val _annotation = c.storageManager.createMemoizedFunction {
        (entry: JetAnnotationEntry) ->
        LazyAnnotationDescriptor(c, entry)
    }

    override fun findAnnotation(fqName: FqName): AnnotationDescriptor? {
        // We can not efficiently check short names here:
        // an annotation class may be renamed on import
        for (annotationDescriptor in iterator()) {
            val annotationType = annotationDescriptor.getType()
            if (annotationType.isError()) continue

            val descriptor = annotationType.getConstructor().getDeclarationDescriptor()
            if (descriptor == null) continue

            if (DescriptorUtils.getFqNameSafe(descriptor) == fqName) {
                return annotationDescriptor
            }
        }

        return null
    }

    override fun iterator(): Iterator<AnnotationDescriptor> {
        return annotationEntries.stream().map(_annotation).iterator()
    }

    override fun forceResolveAllContents() {
        // To resolve all entries
        this.toList()
    }
}

public class LazyAnnotationDescriptor(
        val c: LazyAnnotationsContext,
        val annotationEntry: JetAnnotationEntry
) : AnnotationDescriptor, LazyEntity {

    {
        c.trace.record(BindingContext.ANNOTATION, annotationEntry, this)
    }

    private val _resolutionResults = c.storageManager.createLazyValue {
        val results = c.annotationResolver.resolveAnnotationCall(
                annotationEntry,
                c.scope,
                c.trace
        )
        AnnotationResolver.checkAnnotationType(annotationEntry, c.trace, results)
        results
    }

    private val _type = c.storageManager.createLazyValue {
        c.annotationResolver.resolveAnnotationType(
                c.scope,
                annotationEntry
        )
    }

    override fun getType() = _type()

    private val _valueArguments = c.storageManager.createMemoizedFunctionWithNullableValues @f {
        (valueParameterDescriptor: ValueParameterDescriptor): CompileTimeConstant<*>? ->
        if (!_resolutionResults().isSingleResult()) return@f null

        val resolvedValueArgument = _resolutionResults().getResultingCall().getValueArguments()[valueParameterDescriptor]
        if (resolvedValueArgument == null) return@f null

        AnnotationResolver.getAnnotationArgumentValue(c.trace, valueParameterDescriptor, resolvedValueArgument)
    }

    override fun getValueArgument(valueParameterDescriptor: ValueParameterDescriptor): CompileTimeConstant<out Any?>? {
        return _valueArguments(valueParameterDescriptor)
    }

    override fun getAllValueArguments(): Map<ValueParameterDescriptor, CompileTimeConstant<out Any?>> {
        if (!_resolutionResults().isSingleResult()) return mapOf()

        return _resolutionResults().getResultingCall().getValueArguments().keySet().keysToMapExceptNulls {
            getValueArgument(it)
        }
    }

    override fun forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(getType())
        getAllValueArguments()
    }
}