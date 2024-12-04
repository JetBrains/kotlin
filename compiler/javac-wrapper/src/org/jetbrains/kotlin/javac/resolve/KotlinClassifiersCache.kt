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

package org.jetbrains.kotlin.javac.resolve

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.javac.JavaClassWithClassId
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType

class KotlinClassifiersCache(sourceFiles: Collection<KtFile>,
                             private val javac: JavacWrapper) {

    private val kotlinPackages = hashSetOf<FqName>()
    private val kotlinFacadeClasses = hashMapOf<ClassId, KtFile>()
    private val kotlinClasses: Map<ClassId?, KtClassOrObject?> =
            sourceFiles.flatMap { ktFile ->
                kotlinPackages.add(ktFile.packageFqName)
                val facadeFqName = ktFile.javaFileFacadeFqName
                kotlinFacadeClasses[ClassId(facadeFqName.parent(), facadeFqName.shortName())] = ktFile
                ktFile.declarations
                        .filterIsInstance<KtClassOrObject>()
                        .map { it.computeClassId() to it }
            }.toMap()

    private val classifiers = hashMapOf<ClassId, JavaClass>()

    fun getKotlinClassifier(classId: ClassId) = classifiers[classId] ?: createClassifier(classId)

    fun createMockKotlinClassifier(classifier: KtClassOrObject?,
                                   ktFile: KtFile?,
                                   classId: ClassId) = MockKotlinClassifier(classId,
                                                                            classifier,
                                                                            ktFile,
                                                                            this,
                                                                            javac)
            .apply { classifiers[classId] = this }

    fun hasPackage(packageFqName: FqName) = kotlinPackages.contains(packageFqName)

    private fun createClassifier(classId: ClassId): JavaClass? {
        kotlinFacadeClasses[classId]?.let {
            return createMockKotlinClassifier(null, it, classId)
        }
        if (classId.isNestedClass) {
            classifiers[classId]?.let { return it }
            val pathSegments = classId.relativeClassName.pathSegments().map { it.asString() }
            val outerClassId = ClassId(classId.packageFqName, Name.identifier(pathSegments.first()))
            var outerClass: JavaClass = kotlinClasses[outerClassId]?.let { createMockKotlinClassifier(it, null, outerClassId) } ?: return null

            pathSegments.drop(1).forEach {
                outerClass = outerClass.findInnerClass(Name.identifier(it)) ?: return null
            }

            return outerClass.apply { classifiers[classId] = this }
        }

        val kotlinClassifier = kotlinClasses[classId] ?: return null

        return createMockKotlinClassifier(kotlinClassifier, null, classId)
    }

}

class MockKotlinClassifier(override val classId: ClassId,
                           private val classOrObject: KtClassOrObject?,
                           private val ktFile: KtFile?,
                           private val cache: KotlinClassifiersCache,
                           private val javac: JavacWrapper) : JavaClassWithClassId {

    override val fqName: FqName
        get() = classId.asSingleFqName()

    override val visibility: Visibility
        get() = if (classOrObject == null) {
            Visibilities.Public
        }
        else when (classOrObject.visibilityModifierType()) {
            null, KtTokens.PUBLIC_KEYWORD -> Visibilities.Public
            KtTokens.PRIVATE_KEYWORD -> Visibilities.Private
            KtTokens.PROTECTED_KEYWORD -> Visibilities.Protected
            else -> JavaVisibilities.PackageVisibility
        }

    override val supertypes: Collection<JavaClassifierType>
        get() = if (classOrObject == null) {
            emptyList()
        }
        else javac.kotlinResolver.resolveSupertypes(classOrObject)
                .mapNotNull { javac.getKotlinClassifier(it) ?: javac.findClass(it) }
                .map { MockKotlinClassifierType(it) }

    val innerClasses: Collection<JavaClass>
        get() = classOrObject?.declarations
                        ?.filterIsInstance<KtClassOrObject>()
                        ?.mapNotNull { nestedClassOrObject ->
                            cache.createMockKotlinClassifier(nestedClassOrObject, ktFile, classId.createNestedClassId(nestedClassOrObject.nameAsSafeName))
                        } ?: emptyList()

    override val isFromSource: Boolean
        get() = true

    override val lightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    override val virtualFile: VirtualFile?
        get() = null

    override val name
        get() = fqName.shortNameOrSpecial()

    override fun isFromSourceCodeInScope(scope: SearchScope) = true

    override val innerClassNames
        get() = innerClasses.map(JavaClass::name)

    override fun findInnerClass(name: Name) = innerClasses.find { it.name == name }

    val typeParametersNumber: Int
        get() = classOrObject?.typeParameters?.size ?: 0

    val hasTypeParameters: Boolean
        get() = typeParametersNumber > 0

    fun findField(name: String) = classOrObject?.let { javac.kotlinResolver.findField(it, name) } ?: javac.kotlinResolver.findField(ktFile, name)

    override val isAbstract get() = shouldNotBeCalled()
    override val isStatic get() = shouldNotBeCalled()
    override val isFinal get() = shouldNotBeCalled()
    override val typeParameters get() = shouldNotBeCalled()
    override val outerClass get() = shouldNotBeCalled()
    override val isInterface get() = shouldNotBeCalled()
    override val isAnnotationType get() = shouldNotBeCalled()
    override val isEnum get() = shouldNotBeCalled()
    override val isRecord get() = shouldNotBeCalled()
    override val isSealed: Boolean get() = shouldNotBeCalled()
    override val permittedTypes: Sequence<JavaClassifierType> get() = shouldNotBeCalled()
    override val methods get() = shouldNotBeCalled()
    override val fields get() = shouldNotBeCalled()
    override val constructors get() = shouldNotBeCalled()
    override val recordComponents get() = shouldNotBeCalled()

    override fun hasDefaultConstructor() = shouldNotBeCalled()
    override val annotations get() = shouldNotBeCalled()
    override val isDeprecatedInJavaDoc get() = shouldNotBeCalled()
    override fun findAnnotation(fqName: FqName) = shouldNotBeCalled()
}

class MockKotlinClassifierType(override val classifier: JavaClassifier) : JavaClassifierType {
    override val typeArguments get() = shouldNotBeCalled()
    override val isRaw get() = shouldNotBeCalled()
    override val annotations get() = shouldNotBeCalled()
    override val classifierQualifiedName get() = shouldNotBeCalled()
    override val presentableText get() = shouldNotBeCalled()
    override fun findAnnotation(fqName: FqName) = shouldNotBeCalled()
    override val isDeprecatedInJavaDoc get() = shouldNotBeCalled()
}

class MockKotlinField(private val psiField: PsiField) : JavaField {

    override val initializerValue: Any?
        get() = (psiField.initializer as? PsiLiteralExpression)?.value

    override val name get() = shouldNotBeCalled()
    override val annotations get() = shouldNotBeCalled()
    override val isDeprecatedInJavaDoc get() = shouldNotBeCalled()
    override val isAbstract get() = shouldNotBeCalled()
    override val isStatic get() = shouldNotBeCalled()
    override val isFinal get() = shouldNotBeCalled()
    override val visibility: Visibility get() = shouldNotBeCalled()
    override val containingClass get() = shouldNotBeCalled()
    override val isEnumEntry get() = shouldNotBeCalled()
    override val type get() = shouldNotBeCalled()
    override val hasConstantNotNullInitializer get() = shouldNotBeCalled()
    override fun findAnnotation(fqName: FqName) = shouldNotBeCalled()
    override val isFromSource: Boolean get() = shouldNotBeCalled()
}

private fun KtClassOrObject.computeClassId(): ClassId? =
        containingClassOrObject?.computeClassId()?.createNestedClassId(nameAsSafeName) ?: fqName?.let { ClassId.topLevel(it) }

private fun shouldNotBeCalled(): Nothing = throw UnsupportedOperationException("Should not be called")
