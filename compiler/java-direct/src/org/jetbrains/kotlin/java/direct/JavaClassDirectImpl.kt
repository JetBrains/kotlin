/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Suppress("UNCHECKED_CAST")
class JavaClassDirectImpl(
    node: DirectSyntaxNode,
    source: CharSequence,
    override val outerClass: JavaClass? = null
) : JavaElementDirectImpl(node, source), JavaClass {

    override val name: Name
        get() = Name.identifier(node.children.find { it.type.toString() == "IDENTIFIER" }?.text ?: "<error>")

    override val fqName: FqName?
        get() {
            // TODO: implement based on package and nesting
            return null
        }

    override val isAbstract: Boolean get() = false
    override val isStatic: Boolean get() = false
    override val isFinal: Boolean get() = false
    override val visibility: Visibility get() = Visibilities.Public

    override val typeParameters: List<JavaTypeParameter> get() = emptyList()
    override val supertypes: Collection<JavaClassifierType> get() = emptyList()
    override val innerClassNames: Collection<Name> get() = emptyList()
    override fun findInnerClass(name: Name): JavaClass? = null

    override val isInterface: Boolean get() = node.type.toString() == "INTERFACE"
    override val isAnnotationType: Boolean get() = node.type.toString() == "ANNOTATION_TYPE"
    override val isEnum: Boolean get() = node.type.toString() == "ENUM"
    override val isRecord: Boolean get() = node.type.toString() == "RECORD"
    override val isSealed: Boolean get() = false
    override val permittedTypes: Sequence<JavaClassifierType> get() = emptySequence()
    override val lightClassOriginKind: LightClassOriginKind? get() = null

    override val methods: Collection<JavaMethod> get() = emptyList()
    override val fields: Collection<JavaField> get() = emptyList()
    override val constructors: Collection<JavaConstructor> get() = emptyList()
    override val recordComponents: Collection<JavaRecordComponent> get() = emptyList()

    override fun hasDefaultConstructor(): Boolean = false

    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    override val isFromSource: Boolean get() = true
}
