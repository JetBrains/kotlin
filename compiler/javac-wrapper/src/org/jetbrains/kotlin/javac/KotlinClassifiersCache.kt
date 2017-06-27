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

package org.jetbrains.kotlin.javac

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType

class KotlinClassifiersCache(sourceFiles: Collection<KtFile>,
                             private val javac: JavacWrapper) {

    private val kotlinPackages = hashSetOf<FqName>()
    private val kotlinClasses: Map<ClassId?, KtClassOrObject?> =
            sourceFiles.flatMap { ktFile ->
                kotlinPackages.add(ktFile.packageFqName)
                ktFile.declarations
                        .filterIsInstance<KtClassOrObject>()
                        .map { it.computeClassId() to it }
            }.toMap()

    private val classifiers = hashMapOf<ClassId, JavaClass>()

    fun getKotlinClassifier(classId: ClassId) = classifiers[classId] ?: createClassifier(classId)

    fun createMockKotlinClassifier(classifier: KtClassOrObject,
                                   classId: ClassId) = MockKotlinClassifier(classId,
                                                                            classifier,
                                                                            this,
                                                                            javac)
            .apply { classifiers[classId] = this }

    fun hasPackage(packageFqName: FqName) = kotlinPackages.contains(packageFqName)

    private fun createClassifier(classId: ClassId): JavaClass? {
        if (classId.isNestedClass) {
            classifiers[classId]?.let { return it }
            val pathSegments = classId.relativeClassName.pathSegments().map { it.asString() }
            val outerClassId = ClassId(classId.packageFqName, Name.identifier(pathSegments.first()))
            var outerClass: JavaClass = kotlinClasses[outerClassId]?.let { createMockKotlinClassifier(it, outerClassId) } ?: return null

            pathSegments.drop(1).forEach {
                outerClass = outerClass.findInnerClass(Name.identifier(it)) ?: return null
            }

            return outerClass.apply { classifiers[classId] = this }
        }

        val kotlinClassifier = kotlinClasses[classId] ?: return null

        return createMockKotlinClassifier(kotlinClassifier, classId)
    }

}

class MockKotlinClassifier(val classId: ClassId,
                           private val classOrObject: KtClassOrObject,
                           private val cache: KotlinClassifiersCache,
                           private val javac: JavacWrapper) : VirtualFileBoundJavaClass {

    override val fqName: FqName
        get() = classId.asSingleFqName()

    override val isAbstract: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val isStatic: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val isFinal: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val visibility: Visibility
        get() = when (classOrObject.visibilityModifierType()) {
            null, KtTokens.PUBLIC_KEYWORD -> Visibilities.PUBLIC
            KtTokens.PRIVATE_KEYWORD -> Visibilities.PRIVATE
            KtTokens.PROTECTED_KEYWORD -> Visibilities.PROTECTED
            else -> JavaVisibilities.PACKAGE_VISIBILITY
        }

    override val typeParameters: List<JavaTypeParameter>
        get() = throw UnsupportedOperationException("Should not be called")

    override val supertypes: Collection<JavaClassifierType>
        get() = javac.kotlinSupertypeResolver.resolveSupertypes(classOrObject)
                .mapNotNull { javac.getKotlinClassifier(it) ?: javac.findClass(it) }
                .map { MockKotlinClassifierType(it) }

    val innerClasses: Collection<JavaClass>
        get() = classOrObject.declarations.filterIsInstance<KtClassOrObject>()
                .mapNotNull { nestedClassOrObject ->
                    nestedClassOrObject.computeClassId()?.let {
                        cache.createMockKotlinClassifier(nestedClassOrObject, it)
                    }
                }

    override val outerClass: JavaClass?
        get() = throw UnsupportedOperationException("Should not be called")

    override val isInterface: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val isAnnotationType: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val isEnum: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val lightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    override val virtualFile: VirtualFile?
        get() = null

    override val methods: Collection<JavaMethod>
        get() = throw UnsupportedOperationException("Should not be called")

    override val fields: Collection<JavaField>
        get() = classOrObject.declarations
                .filterIsInstance<KtProperty>()
                .map(::MockKotlinField) + classOrObject.companionObjects.flatMap {
            it.declarations
                    .filterIsInstance<KtProperty>()
                    .map(::MockKotlinField)
        }

    override val constructors: Collection<JavaConstructor>
        get() = throw UnsupportedOperationException("Should not be called")

    override val name
        get() = fqName.shortNameOrSpecial()

    override val annotations
        get() = throw UnsupportedOperationException("Should not be called")

    override val isDeprecatedInJavaDoc: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override fun isFromSourceCodeInScope(scope: SearchScope) = true

    override fun findAnnotation(fqName: FqName) =
            throw UnsupportedOperationException("Should not be called")

    override val innerClassNames
        get() = innerClasses.map(JavaClass::name)

    override fun findInnerClass(name: Name) =
            innerClasses.find { it.name == name }

    val typeParametersNumber: Int
        get() = classOrObject.typeParameters.size

    val hasTypeParameters: Boolean
        get() = typeParametersNumber > 0

}

class MockKotlinClassifierType(override val classifier: JavaClassifier) : JavaClassifierType {

    override val typeArguments: List<JavaType>
        get() = throw UnsupportedOperationException("Should not be called")

    override val isRaw: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val annotations: Collection<JavaAnnotation>
        get() = throw UnsupportedOperationException("Should not be called")

    override val classifierQualifiedName: String
        get() = throw UnsupportedOperationException("Should not be called")

    override val presentableText: String
        get() = throw UnsupportedOperationException("Should not be called")

    override fun findAnnotation(fqName: FqName) =
            throw UnsupportedOperationException("Should not be called")

    override val isDeprecatedInJavaDoc: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

}

class MockKotlinField(private val property: KtProperty) : JavaField {
    override val name: Name
        get() = property.nameAsSafeName
    override val annotations: Collection<JavaAnnotation>
        get() = throw UnsupportedOperationException("Should not be called")
    override val isDeprecatedInJavaDoc: Boolean
        get() = throw UnsupportedOperationException("Should not be called")
    override val isAbstract: Boolean
        get() = throw UnsupportedOperationException("Should not be called")
    override val isStatic: Boolean
        get() = throw UnsupportedOperationException("Should not be called")
    override val isFinal: Boolean
        get() = throw UnsupportedOperationException("Should not be called")
    override val visibility: Visibility
        get() = throw UnsupportedOperationException("Should not be called")
    override val containingClass: JavaClass
        get() = throw UnsupportedOperationException("Should not be called")
    override val isEnumEntry: Boolean
        get() = throw UnsupportedOperationException("Should not be called")
    override val type: JavaType
        get() = throw UnsupportedOperationException("Should not be called")
    override val initializerValue: Any?
        get() {
            if (!property.hasModifier(KtTokens.CONST_KEYWORD)) return null
            val initializer = property.initializer ?: return null

            return initializer.text.toIntOrNull() ?: initializer.text
        }
    override val hasConstantNotNullInitializer: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override fun findAnnotation(fqName: FqName) = throw UnsupportedOperationException("Should not be called")
}

private fun KtClassOrObject.computeClassId(): ClassId? =
        containingClassOrObject?.computeClassId()?.createNestedClassId(nameAsSafeName) ?: fqName?.let { ClassId.topLevel(it) }