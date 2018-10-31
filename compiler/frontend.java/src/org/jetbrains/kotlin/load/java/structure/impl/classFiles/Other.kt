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

import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor

class BinaryJavaField(
        override val name: Name,
        override val access: Int,
        override val containingClass: JavaClass,
        override val isEnumEntry: Boolean,
        override val type: JavaType,
        override val initializerValue: Any?
) : JavaField, MapBasedJavaAnnotationOwner, BinaryJavaModifierListOwner {
    override val annotations: MutableCollection<JavaAnnotation> = ContainerUtil.newSmartList()
    override val annotationsByFqName by buildLazyValueForMap()

    override val hasConstantNotNullInitializer: Boolean
        get() = initializerValue != null
}

class BinaryJavaTypeParameter(
        override val name: Name,
        override val upperBounds: Collection<JavaClassifierType>
) : JavaTypeParameter {
    // TODO: support annotations on type parameters
    override val annotations get() = emptyList<JavaAnnotation>()
    override fun findAnnotation(fqName: FqName) = null

    override val isDeprecatedInJavaDoc get() = false
}

class BinaryJavaValueParameter(
        override val type: JavaType,
        override val isVararg: Boolean
) : JavaValueParameter, MapBasedJavaAnnotationOwner {
    override val annotations: MutableCollection<JavaAnnotation> = ContainerUtil.newSmartList()
    override val annotationsByFqName by buildLazyValueForMap()

    override var name: Name? = null

    internal fun updateName(newName: Name) {
        assert(name == null) { "Parameter can't have two names: $name and $newName" }
        name = newName
    }
}

fun isNotTopLevelClass(classContent: ByteArray): Boolean {
    var isNotTopLevelClass = false
    ClassReader(classContent).accept(
        object : ClassVisitor(ASM_API_VERSION_FOR_CLASS_READING) {
            private var internalName: String? = null
            override fun visit(
                version: Int,
                access: Int,
                name: String?,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                internalName = name
            }

            override fun visitInnerClass(name: String?, outerName: String?, innerName: String?, access: Int) {
                // Do not read InnerClasses attribute values where full name != outer + $ + inner; treat those classes as top level instead.
                if (name == internalName && (innerName == null || name == "$outerName$$innerName")) {
                    isNotTopLevelClass = true
                }
            }
        },
        ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
    )
    return isNotTopLevelClass
}
