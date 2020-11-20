/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

// They are only used for java class files, but potentially may be used in other cases
// It would be better to call them like JavaSomeTypeImpl, but these names are already occupied by the PSI based types
internal class PlainJavaArrayType(override val componentType: JavaType) : JavaArrayType
internal class PlainJavaWildcardType(override val bound: JavaType?, override val isExtends: Boolean) : JavaWildcardType
internal class PlainJavaPrimitiveType(override val type: PrimitiveType?) : JavaPrimitiveType

internal class PlainJavaClassifierType(
        // calculation of classifier and canonicalText
        classifierComputation: () -> ClassifierResolutionContext.Result,
        override val typeArguments: List<JavaType>
) : JavaClassifierType {
    private val classifierResolverResult by lazy(LazyThreadSafetyMode.NONE, classifierComputation)

    override val classifier get() = classifierResolverResult.classifier
    override val isRaw
        get() = typeArguments.isEmpty() &&
                classifierResolverResult.classifier?.safeAs<JavaClass>()?.typeParameters?.isNotEmpty() == true

    private var _annotations = emptyList<JavaAnnotation>()
    override val annotations get() = _annotations

    override fun findAnnotation(fqName: FqName) = annotations.find { it.classId?.asSingleFqName() == fqName }

    internal fun addAnnotation(annotation: JavaAnnotation) {
        if (_annotations.isEmpty()) {
            _annotations = SmartList()
        }

        (_annotations as MutableList).add(annotation)
    }

    override val isDeprecatedInJavaDoc get() = false

    override val classifierQualifiedName: String
        get() = classifierResolverResult.qualifiedName

    // TODO: render arguments for presentable text
    override val presentableText: String
        get() = classifierQualifiedName
}
