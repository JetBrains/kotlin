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

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.javac.wrappers.trees.find
import org.jetbrains.kotlin.javac.wrappers.trees.findInner
import org.jetbrains.kotlin.javac.wrappers.trees.tryToResolveByFqName
import org.jetbrains.kotlin.javac.wrappers.trees.tryToResolveInJavaLang

class KotlinClassifiersCache(sourceFiles: Collection<KtFile>,
                             private val javac: JavacWrapper) {

    private val kotlinClasses: Map<FqName?, KtClassOrObject?> = sourceFiles.flatMap {
        (it.collectDescendantsOfType<KtClassOrObject>()
                .map { it.fqName to it } + (it.javaFileFacadeFqName to null))
    }.toMap()

    private val classifiers = hashMapOf<FqName, JavaClass>()

    fun getKotlinClassifier(fqName: FqName) = classifiers[fqName] ?: createClassifier(fqName)

    private fun createClassifier(fqName: FqName): JavaClass? {
        if (!kotlinClasses.containsKey(fqName)) return null
        val kotlinClassifier = kotlinClasses[fqName] ?: return null

        return MockKotlinClassifier(fqName,
                                    kotlinClassifier,
                                    kotlinClassifier.typeParameters.isNotEmpty(),
                                    javac)
                .apply { classifiers[fqName] = this }
    }

}

class MockKotlinClassifier(override val fqName: FqName,
                           private val classOrObject: KtClassOrObject,
                           val hasTypeParameters: Boolean,
                           private val javac: JavacWrapper) : JavaClass {

    override val isAbstract: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val isStatic: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val isFinal: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override val visibility: Visibility
        get() = throw UnsupportedOperationException("Should not be called")

    override val typeParameters: List<JavaTypeParameter>
        get() = throw UnsupportedOperationException("Should not be called")

    override val supertypes: Collection<JavaClassifierType>
        get() = classOrObject.superTypeListEntries
                .mapNotNull {
                    val userType = it.typeAsUserType
                    arrayListOf<String>().apply {
                        userType?.referencedName?.let { add(it) }
                        var qualifier = userType?.qualifier
                        while (qualifier != null) {
                            qualifier.referencedName?.let { add(it) }
                            qualifier = qualifier.qualifier
                        }
                    }
                            .reversed()
                            .joinToString(separator = ".") { it }
                }
                .mapNotNull { resolveSupertype(it, classOrObject, javac) }
                .map { MockKotlinClassifierType(it) }

    val innerClasses: Collection<JavaClass>
        get() = classOrObject.declarations.filterIsInstance<KtClassOrObject>()
                .mapNotNull { it.fqName?.let { javac.getKotlinClassifier(it) } }

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

    override val methods: Collection<JavaMethod>
        get() = throw UnsupportedOperationException("Should not be called")

    override val fields: Collection<JavaField>
        get() = throw UnsupportedOperationException("Should not be called")

    override val constructors: Collection<JavaConstructor>
        get() = throw UnsupportedOperationException("Should not be called")

    override val name
        get() = fqName.shortNameOrSpecial()

    override val annotations
        get() = throw UnsupportedOperationException("Should not be called")

    override val isDeprecatedInJavaDoc: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

    override fun findAnnotation(fqName: FqName) = throw UnsupportedOperationException("Should not be called")

    override val innerClassNames
        get() = innerClasses.map(JavaClass::name)

    override fun findInnerClass(name: Name) = innerClasses.find { it.name == name }

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

    override fun findAnnotation(fqName: FqName) = throw UnsupportedOperationException("Should not be called")

    override val isDeprecatedInJavaDoc: Boolean
        get() = throw UnsupportedOperationException("Should not be called")

}

private fun resolveSupertype(name: String,
                             classOrObject: KtClassOrObject,
                             javac: JavacWrapper): JavaClass? {
    val nameParts = name.split(".")
    val ktFile = classOrObject.containingKtFile

    tryToResolveInner(name, classOrObject, javac, nameParts)?.let { return it }
    ktFile.tryToResolvePackageClass(name, javac, nameParts)?.let { return it }
    tryToResolveByFqName(name, javac)?.let { return it }
    ktFile.tryToResolveSingleTypeImport(name, javac, nameParts)?.let { return it }
    ktFile.tryToResolveTypeImportOnDemand(name, javac, nameParts)?.let { return it }
    tryToResolveInJavaLang(name, javac)?.let { return it }

    return null
}

private fun tryToResolveInner(name: String,
                              classOrObject: KtClassOrObject,
                              javac: JavacWrapper,
                              nameParts: List<String>) = classOrObject.containingClassOrObject
        ?.let { it.fqName?.let { javac.findClass(it) ?: javac.getKotlinClassifier(it) } }
        ?.findInner(name, javac, nameParts)

private fun KtFile.tryToResolvePackageClass(name: String,
                                     javac: JavacWrapper,
                                     nameParts: List<String> = emptyList()): JavaClass? {
    if (nameParts.size > 1) {
        return find(FqName("${packageFqName.asString()}.${nameParts.first()}"), javac, nameParts)
    } else return (javac.findClass(FqName("${packageFqName.asString()}.$name")) ?: javac.getKotlinClassifier(FqName("${packageFqName.asString()}.$name")))
            ?.let { return it }
}

private fun KtFile.tryToResolveSingleTypeImport(name: String,
                                                javac: JavacWrapper,
                                                nameParts: List<String> = emptyList()): JavaClass? {
    if (nameParts.size > 1) {
        val foundImports = importDirectives.filter { it.text.endsWith(".${nameParts.first()}") }
        foundImports.forEach {
            it.importedFqName?.let { find(it, javac, nameParts)?.let { return it } }
        }
        return null
    } else return importDirectives
            .find { it.text.endsWith(".$name") }
            ?.let {
                it.importedFqName?.let { javac.findClass(it) ?: javac.getKotlinClassifier(it) }
            }
}

private fun KtFile.tryToResolveTypeImportOnDemand(name: String,
                                                  javac: JavacWrapper,
                                                  nameParts: List<String> = emptyList()): JavaClass? {
    val packagesWithAsterisk = importDirectives.filter { it.text.endsWith("*") }

    if (nameParts.size > 1) {
        packagesWithAsterisk.forEach { pack ->
            find(FqName("${pack.importedFqName?.asString()}.${nameParts.first()}"), javac, nameParts)
                    ?.let { return it }
        }
        return null
    } else {
        packagesWithAsterisk
                .forEach {
                    val fqName = "${it.importedFqName?.asString()}.$name".let(::FqName)
                    javac.findClass(fqName)?.let { return it } ?: javac.getKotlinClassifier(fqName)?.let { return it }
                }

        return null
    }
}