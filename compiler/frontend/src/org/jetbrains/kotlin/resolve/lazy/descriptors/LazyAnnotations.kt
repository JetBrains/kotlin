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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.LazyEntity
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.isError

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

class LazyAnnotations(
        val c: LazyAnnotationsContext,
        val annotationEntries: List<KtAnnotationEntry>
) : Annotations, LazyEntity {
    override fun isEmpty() = annotationEntries.isEmpty()

    private val annotation = c.storageManager.createMemoizedFunction {
        entry: KtAnnotationEntry ->

        val descriptor = LazyAnnotationDescriptor(c, entry)
        val target = entry.useSiteTarget?.getAnnotationUseSiteTarget()
        AnnotationWithTarget(descriptor, target)
    }

    override fun findAnnotation(fqName: FqName): AnnotationDescriptor? {
        // We can not efficiently check short names here:
        // an annotation class may be renamed on import
        for (annotationDescriptor in iterator()) {
            val annotationType = annotationDescriptor.type
            if (annotationType.isError) continue

            val descriptor = annotationType.constructor.declarationDescriptor ?: continue

            if (DescriptorUtils.getFqNameSafe(descriptor) == fqName) {
                return annotationDescriptor
            }
        }

        return null
    }

    override fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> {
        return annotationEntries
                .mapNotNull {
                    val (descriptor, target) = annotation(it)
                    if (target == null) null else AnnotationWithTarget(descriptor, target)
                }
    }

    override fun getAllAnnotations() = annotationEntries.map(annotation)

    override fun iterator(): Iterator<AnnotationDescriptor> {
        return annotationEntries
                .asSequence()
                .mapNotNull {
                    val (descriptor, target) = annotation(it)
                    if (target == null) descriptor else null // Filter out annotations with target
                }.iterator()
    }

    override fun forceResolveAllContents() {
        // To resolve all entries
        getAllAnnotations()
    }
}

class LazyAnnotationDescriptor(
        val c: LazyAnnotationsContext,
        val annotationEntry: KtAnnotationEntry
) : AnnotationDescriptor, LazyEntity {

    init {
        c.trace.record(BindingContext.ANNOTATION, annotationEntry, this)
    }

    override val type by c.storageManager.createLazyValue {
        c.annotationResolver.resolveAnnotationType(scope, annotationEntry, c.trace)
    }

    override val source = annotationEntry.toSourceElement()

    private val scope = if (c.scope.ownerDescriptor is PackageFragmentDescriptor) {
        LexicalScope.Base(c.scope, FileDescriptorForVisibilityChecks(source, c.scope.ownerDescriptor))
    }
    else {
        c.scope
    }

    override val allValueArguments by c.storageManager.createLazyValue {
        val resolutionResults = c.annotationResolver.resolveAnnotationCall(annotationEntry, scope, c.trace)
        AnnotationResolverImpl.checkAnnotationType(annotationEntry, c.trace, resolutionResults)

        if (!resolutionResults.isSingleResult) return@createLazyValue emptyMap<ValueParameterDescriptor, ConstantValue<*>>()

        @Suppress("UNCHECKED_CAST")
        resolutionResults.resultingCall.valueArguments
                .mapValues { val (valueParameter, resolvedArgument) = it
                    if (resolvedArgument == null) null
                    else c.annotationResolver.getAnnotationArgumentValue(c.trace, valueParameter, resolvedArgument)
                }
                .filterValues { it != null } as Map<ValueParameterDescriptor, ConstantValue<*>>
    }

    override fun forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(type)
        allValueArguments
    }

    private class FileDescriptorForVisibilityChecks(
            private val source: SourceElement,
            private val containingDeclaration: DeclarationDescriptor
    ) : DeclarationDescriptorWithSource {
        override val annotations: Annotations get() = Annotations.EMPTY
        override fun getContainingDeclaration() = containingDeclaration
        override fun getSource() = source
        override fun getOriginal() = this
        override fun getName() = Name.special("< file descriptor for annotation resolution >")

        private fun error(): Nothing = error("This method should not be called")
        override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R = error()
        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) = error()

        override fun toString(): String = "${name.asString()} declared in LazyAnnotations.kt"
    }
}
