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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetAnnotationEntry
import org.jetbrains.kotlin.psi.JetAnnotationUseSiteTarget
import org.jetbrains.kotlin.resolve.AnnotationResolver
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.LazyEntity
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.storage.StorageManager

abstract class LazyAnnotationsContext(
        val annotationResolver: AnnotationResolver,
        val storageManager: StorageManager,
        val trace: BindingTrace
) {
    abstract val scope: LexicalScope
}

class LazyAnnotationsContextImpl(
        annotationResolver: AnnotationResolver,
        storageManager: StorageManager,
        trace: BindingTrace,
        override val scope: LexicalScope
) : LazyAnnotationsContext(annotationResolver, storageManager, trace)

public class LazyAnnotations(
        val c: LazyAnnotationsContext,
        val annotationEntries: List<JetAnnotationEntry>
) : Annotations, LazyEntity {
    override fun isEmpty() = annotationEntries.isEmpty()

    private val annotation = c.storageManager.createMemoizedFunction {
        entry: JetAnnotationEntry ->

        val descriptor = LazyAnnotationDescriptor(c, entry)
        val target = entry.getUseSiteTarget()?.getAnnotationUseSiteTarget()
        AnnotationWithTarget(descriptor, target)
    }

    override fun findAnnotation(fqName: FqName): AnnotationDescriptor? {
        // We can not efficiently check short names here:
        // an annotation class may be renamed on import
        for (annotationDescriptor in iterator()) {
            val annotationType = annotationDescriptor.getType()
            if (annotationType.isError()) continue

            val descriptor = annotationType.getConstructor().getDeclarationDescriptor() ?: continue

            if (DescriptorUtils.getFqNameSafe(descriptor) == fqName) {
                return annotationDescriptor
            }
        }

        return null
    }

    override fun findExternalAnnotation(fqName: FqName) = null

    override fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> {
        return annotationEntries
                .asSequence()
                .map {
                    val (descriptor, target) = annotation(it)
                    if (target == null) null else AnnotationWithTarget(descriptor, target)
                }.filterNotNull().toList()
    }

    override fun getAllAnnotations() = annotationEntries.map(annotation)

    override fun iterator(): Iterator<AnnotationDescriptor> {
        return annotationEntries
                .asSequence()
                .map {
                    val (descriptor, target) = annotation(it)
                    if (target == null) descriptor else null // Filter out annotations with target
                }.filterNotNull().iterator()
    }

    override fun forceResolveAllContents() {
        // To resolve all entries
        getAllAnnotations()
    }
}

public class LazyAnnotationDescriptor(
        val c: LazyAnnotationsContext,
        val annotationEntry: JetAnnotationEntry
) : AnnotationDescriptor, LazyEntity {

    init {
        c.trace.record(BindingContext.ANNOTATION, annotationEntry, this)
    }

    private val type = c.storageManager.createLazyValue {
        c.annotationResolver.resolveAnnotationType(
                c.scope,
                annotationEntry,
                c.trace
        )
    }

    private val valueArguments = c.storageManager.createLazyValue {
        computeValueArguments()
    }

    private val source = annotationEntry.toSourceElement()

    override fun getType() = type()

    override fun getAllValueArguments() = valueArguments()

    private fun computeValueArguments(): Map<ValueParameterDescriptor, ConstantValue<*>> {
        val resolutionResults = c.annotationResolver.resolveAnnotationCall(annotationEntry, c.scope, c.trace)
        AnnotationResolver.checkAnnotationType(annotationEntry, c.trace, resolutionResults)

        if (!resolutionResults.isSingleResult()) return mapOf()

        @Suppress("UNCHECKED_CAST")
        return resolutionResults.getResultingCall().getValueArguments()
                .mapValues { val (valueParameter, resolvedArgument) = it;
                    if (resolvedArgument == null) null
                    else c.annotationResolver.getAnnotationArgumentValue(c.trace, valueParameter, resolvedArgument)
                }
                .filterValues { it != null } as Map<ValueParameterDescriptor, ConstantValue<*>>
    }

    override fun getSource() = source

    override fun forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(getType())
        getAllValueArguments()
    }
}
