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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.load.java.components.RuntimeSourceElementFactory
import org.jetbrains.kotlin.load.java.structure.reflect.ReflectJavaAnnotation
import org.jetbrains.kotlin.load.kotlin.reflect.ReflectAnnotationSource
import kotlin.reflect.KAnnotatedElement

internal interface KAnnotatedElementImpl : KAnnotatedElement {
    val annotated: Annotated

    override val annotations: List<Annotation>
        get() = annotated.annotations.mapNotNull {
            val source = it.source
            when (source) {
                is ReflectAnnotationSource -> {
                    source.annotation
                }
                is RuntimeSourceElementFactory.RuntimeSourceElement -> {
                    (source.javaElement as? ReflectJavaAnnotation)?.annotation
                }
                else -> null
            }
        }
}
