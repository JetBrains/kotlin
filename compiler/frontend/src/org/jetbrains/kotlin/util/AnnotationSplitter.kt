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

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import kotlin.platform.platformStatic

public class AnnotationSplitter(original: Annotations, applicableTargets: Set<AnnotationUseSiteTarget>) {
    private val annotations: Map<AnnotationUseSiteTarget, List<AnnotationWithTarget>>
    public val otherAnnotations: Annotations

    init {
        val map = hashMapOf<AnnotationUseSiteTarget, MutableList<AnnotationWithTarget>>()
        val other = arrayListOf<AnnotationWithTarget>()

        outer@ for (annotationWithTarget in original.getAllAnnotations()) {
            val useSiteTarget = annotationWithTarget.target
            if (useSiteTarget != null) {
                if (useSiteTarget in applicableTargets)
                    map.getOrPut(useSiteTarget, { arrayListOf() }).add(annotationWithTarget)
                else
                    other.add(annotationWithTarget)

                continue@outer
            }

            for (target in TARGET_PRIORITIES) {
                if (target !in applicableTargets) continue

                val declarationSiteTargetForCurrentTarget = KotlinTarget.USE_SITE_MAPPING[target] ?: continue
                val applicableTargetsForAnnotation = AnnotationChecker.applicableTargetSet(annotationWithTarget.annotation)
                val applicable = applicableTargetsForAnnotation.any { it == declarationSiteTargetForCurrentTarget }

                if (applicable) {
                    map.getOrPut(target, { arrayListOf() }).add(annotationWithTarget)
                    continue@outer
                }
            }

            other.add(annotationWithTarget)
        }
        annotations = map
        otherAnnotations = AnnotationsImpl.create(other)
    }

    public fun getAnnotationsForTarget(target: AnnotationUseSiteTarget): Annotations {
        val annotations = annotations[target] ?: return Annotations.EMPTY
        return AnnotationsImpl.create(annotations)
    }

    public fun getAnnotationsForTargets(vararg targets: AnnotationUseSiteTarget): Annotations {
        return CompositeAnnotations(targets.map { getAnnotationsForTarget(it) })
    }

    public companion object {
        private val TARGET_PRIORITIES = setOf(CONSTRUCTOR_PARAMETER, FIELD, PROPERTY, PROPERTY_SETTER, PROPERTY_GETTER)

        platformStatic
        public fun create(original: Annotations, parameter: Boolean, hasBackingField: Boolean, isMutable: Boolean): AnnotationSplitter {
            return AnnotationSplitter(original, with(hashSetOf(PROPERTY, PROPERTY_GETTER)) {
                if (parameter) add(CONSTRUCTOR_PARAMETER)
                if (hasBackingField) add(FIELD)
                if (isMutable) add(PROPERTY_SETTER)
                this
            })
        }
    }

}