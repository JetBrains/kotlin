/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.javac.wrappers.symbols

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.javac.JavaClassWithClassId
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.tools.JavaFileObject

// This represents Java class for which we don't have resolved classifier.
// The situation when it is useful is described in KT-33932.
// Mostly it's a stub.
class FakeSymbolBasedClass(
    element: TypeElement,
    javac: JavacWrapper,
    override val classId: ClassId?,
    val file: JavaFileObject?
) : SymbolBasedClassifier<TypeElement>(element, javac), JavaClassWithClassId {

    override val name: Name get() = Name.identifier(element.simpleName.toString())

    override val isAbstract: Boolean get() = true

    override val isStatic: Boolean get() = false

    override val isFinal: Boolean get() = false

    override val visibility: Visibility get() = Visibilities.Public

    override val typeParameters: List<JavaTypeParameter> get() = emptyList()

    override val fqName: FqName get() = FqName(element.qualifiedName.toString())

    override val supertypes: Collection<JavaClassifierType> get() = emptyList()

    val innerClasses: Map<Name, JavaClass> get() = emptyMap()

    override val outerClass: JavaClass?
            by lazy {
                element.enclosingElement?.let {
                    if (it.asType().kind != TypeKind.DECLARED) null else FakeSymbolBasedClass(
                        it as TypeElement,
                        javac,
                        classId?.outerClassId,
                        file
                    )
                }
            }

    override val isInterface: Boolean get() = true

    override val isAnnotationType: Boolean get() = false

    override val isEnum: Boolean get() = false

    override val isRecord: Boolean get() = false

    override val recordComponents: Collection<JavaRecordComponent> get() = emptyList()

    override val isSealed: Boolean get() = false

    override val permittedTypes: Sequence<JavaClassifierType> get() = emptySequence()

    override val lightClassOriginKind: LightClassOriginKind? get() = null

    override val methods: Collection<JavaMethod> get() = emptyList()

    override val fields: Collection<JavaField> get() = emptyList()

    override val constructors: Collection<JavaConstructor> get() = emptyList()

    override fun hasDefaultConstructor() = false

    override val innerClassNames: Collection<Name> get() = emptyList()

    override val virtualFile: VirtualFile? by lazy {
        file?.let { javac.toVirtualFile(it) }
    }

    override fun isFromSourceCodeInScope(scope: SearchScope): Boolean = false

    override fun findInnerClass(name: Name): JavaClass? = null
}
