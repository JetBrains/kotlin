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
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.FilteredByPredicateAnnotations
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.resolve.AnnotationResolver
import org.jetbrains.kotlin.resolve.AnnotationResolverImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.LazyEntity
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.AbbreviatedType
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

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

    private val annotation = c.storageManager.createMemoizedFunction { entry: KtAnnotationEntry ->
        LazyAnnotationDescriptor(c, entry)
    }

    override fun iterator(): Iterator<AnnotationDescriptor> = annotationEntries.asSequence().map(annotation).iterator()

    override fun forceResolveAllContents() {
        // To resolve all entries
        for (annotation in this) {
            // TODO: probably we should do ForceResolveUtil.forceResolveAllContents(annotation) here
        }
    }
}

class LazyAnnotationDescriptor(
    val c: LazyAnnotationsContext,
    val annotationEntry: KtAnnotationEntry
) : AnnotationDescriptor, LazyEntity {

    init {
        c.trace.record(BindingContext.ANNOTATION, annotationEntry, this)
    }

    override val type by c.storageManager.createLazyValue(
        computable = lazy@{
            val annotationType = c.annotationResolver.resolveAnnotationType(scope, annotationEntry, c.trace)
            if (annotationType is AbbreviatedType) {
                // This is needed to prevent recursion in cases like this: typealias S = @S Ann
                if (annotationType.annotations.any { it == this }) {
                    annotationType.abbreviation.constructor.declarationDescriptor?.let { typeAliasDescriptor ->
                        c.trace.report(Errors.RECURSIVE_TYPEALIAS_EXPANSION.on(annotationEntry, typeAliasDescriptor))
                    }
                    return@lazy annotationType.replaceAnnotations(FilteredByPredicateAnnotations(annotationType.annotations) { it != this })
                }
            }
            annotationType
        },
        onRecursiveCall = {
            ErrorUtils.createErrorType(ErrorTypeKind.RECURSIVE_ANNOTATION_TYPE)
        }
    )

    override val source = annotationEntry.toSourceElement()

    private val scope = (c.scope.ownerDescriptor as? PackageFragmentDescriptor)?.let {
        LexicalScope.Base(c.scope, FileDescriptorForVisibilityChecks(source, it))
    } ?: c.scope

    override val allValueArguments by c.storageManager.createLazyValue {
        val resolutionResults = c.annotationResolver.resolveAnnotationCall(annotationEntry, scope, c.trace)
        AnnotationResolverImpl.checkAnnotationType(annotationEntry, c.trace, resolutionResults)

        if (!resolutionResults.isSingleResult) return@createLazyValue emptyMap<Name, ConstantValue<*>>()

        resolutionResults.resultingCall.valueArguments.mapNotNull { (valueParameter, resolvedArgument) ->
            if (resolvedArgument == null) null
            else c.annotationResolver.getAnnotationArgumentValue(c.trace, valueParameter, resolvedArgument)?.let { value ->
                valueParameter.name to value
            }
        }.toMap()
    }

    override fun forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(type)
        allValueArguments
    }

    private class FileDescriptorForVisibilityChecks(
        private val source: SourceElement,
        private val containingDeclaration: PackageFragmentDescriptor
    ) : DeclarationDescriptorWithSource, PackageFragmentDescriptor by containingDeclaration {
        override val annotations: Annotations get() = Annotations.EMPTY
        override fun getSource() = source
        override fun getOriginal() = this
        override fun getName() = Name.special("< file descriptor for annotation resolution >")

        private fun error(): Nothing = error("This method should not be called")
        override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R = error()
        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) = error()

        override fun toString(): String = "${name.asString()} declared in LazyAnnotations.kt"
    }
}
