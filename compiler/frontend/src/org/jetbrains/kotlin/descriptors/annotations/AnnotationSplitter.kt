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

package org.jetbrains.kotlin.descriptors.annotations

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

/*
    This class lazily splits Annotations into several different Annotations according to declaration site target priority on property.
    Basically you pass an original Annotations and a list of targets applicable for your declaration.

    Example: Annotations = [@property:P1, @field:F1, F2, P2, T],
             F2, P2, T are annotations with declaration site targets 'field', 'property' and 'type', respectively.

    Annotations will be split as:
             FIELD -> [F1, F2], (even if F2 is applicable to PROPERTY because of target priority)
             PROPERTY -> [P1, P2],
             other -> [T].
 */

public class AnnotationSplitter(
        val storageManager: StorageManager,
        allAnnotations: Annotations,
        applicableTargetsLazy: () -> Set<AnnotationUseSiteTarget>
) {
    public companion object {
        private val TARGET_PRIORITIES = setOf(CONSTRUCTOR_PARAMETER, PROPERTY, FIELD)

        @JvmStatic
        public fun create(
                storageManager: StorageManager,
                annotations: Annotations,
                targets: Set<AnnotationUseSiteTarget>
        ): AnnotationSplitter {
            return AnnotationSplitter(storageManager, annotations, { targets })
        }

        @JvmStatic
        public fun getTargetSet(parameter: Boolean, context: BindingContext, wrapper: PropertyWrapper): Set<AnnotationUseSiteTarget> {
            val property = wrapper.property
            assert(property != null)
            val hasBackingField = context[BindingContext.BACKING_FIELD_REQUIRED, property] ?: false
            return getTargetSet(parameter, property!!.isVar, hasBackingField)
        }

        @JvmStatic
        public fun getTargetSet(parameter: Boolean, isVar: Boolean, hasBackingField: Boolean): Set<AnnotationUseSiteTarget> {
            return hashSetOf(PROPERTY, PROPERTY_GETTER).apply {
                if (parameter) add(CONSTRUCTOR_PARAMETER)
                if (hasBackingField) add(FIELD)
                if (isVar) add(PROPERTY_SETTER)
            }
        }
    }

    public class PropertyWrapper(public var property: PropertyDescriptor? = null)

    private val splitAnnotations = storageManager.createLazyValue {
        val map = hashMapOf<AnnotationUseSiteTarget, MutableList<AnnotationWithTarget>>()
        val other = arrayListOf<AnnotationWithTarget>()
        val applicableTargets = applicableTargetsLazy()
        val applicableTargetsWithoutUseSiteTarget = applicableTargets.intersect(TARGET_PRIORITIES)

        outer@ for (annotationWithTarget in allAnnotations.getAllAnnotations()) {
            val useSiteTarget = annotationWithTarget.target
            if (useSiteTarget != null) {
                if (useSiteTarget in applicableTargets)
                    map.getOrPut(useSiteTarget, { arrayListOf() }).add(annotationWithTarget)
                else
                    other.add(annotationWithTarget)

                continue@outer
            }

            for (target in TARGET_PRIORITIES) {
                if (target !in applicableTargetsWithoutUseSiteTarget) continue

                val declarationSiteTargetForCurrentTarget = KotlinTarget.USE_SITE_MAPPING[target] ?: continue
                val applicableTargetsForAnnotation = AnnotationChecker.applicableTargetSet(annotationWithTarget.annotation)

                if (declarationSiteTargetForCurrentTarget in applicableTargetsForAnnotation) {
                    map.getOrPut(target, { arrayListOf() }).add(annotationWithTarget)
                    continue@outer
                }
            }

            other.add(annotationWithTarget)
        }
        map to AnnotationsImpl.create(other)
    }

    public fun getOtherAnnotations(): Annotations = LazySplitAnnotations(storageManager, null)

    public fun getAnnotationsForTarget(target: AnnotationUseSiteTarget): Annotations = LazySplitAnnotations(storageManager, target)

    public fun getAnnotationsForTargets(vararg targets: AnnotationUseSiteTarget): Annotations {
        return CompositeAnnotations(targets.map { getAnnotationsForTarget(it) })
    }

    private inner class LazySplitAnnotations(storageManager: StorageManager, val target: AnnotationUseSiteTarget?) : Annotations {
        private val annotations by storageManager.createLazyValue {
            val splitAnnotations = this@AnnotationSplitter.splitAnnotations()

            if (target != null)
                AnnotationsImpl.create(splitAnnotations.first[target] ?: emptyList())
            else
                splitAnnotations.second
        }

        override fun isEmpty() = annotations.isEmpty()
        override fun findAnnotation(fqName: FqName) = annotations.findAnnotation(fqName)
        override fun getUseSiteTargetedAnnotations() = annotations.getUseSiteTargetedAnnotations()
        override fun getAllAnnotations() = annotations.getAllAnnotations()
        override fun findExternalAnnotation(fqName: FqName) = annotations.findExternalAnnotation(fqName)
        override fun iterator() = annotations.iterator()
        override fun toString() = annotations.toString()
    }

}