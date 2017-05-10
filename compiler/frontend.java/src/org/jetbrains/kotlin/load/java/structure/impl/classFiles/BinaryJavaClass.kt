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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import gnu.trove.THashMap
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.org.objectweb.asm.*
import java.text.CharacterIterator
import java.text.StringCharacterIterator

class BinaryJavaClass(
        override val virtualFile: VirtualFile,
        override val fqName: FqName,
        private val context: ClassifierResolutionContext,
        private val signatureParser: BinaryClassSignatureParser,
        override var access: Int = 0,
        override val outerClass: JavaClass?,
        classContent: ByteArray? = null
) : ClassVisitor(ASM_API_VERSION_FOR_CLASS_READING), VirtualFileBoundJavaClass, BinaryJavaModifierListOwner, BinaryJavaAnnotationOwner {
    lateinit var myInternalName: String

    override val annotations: MutableCollection<JavaAnnotation> = mutableListOf()
    override lateinit var typeParameters: List<JavaTypeParameter>
    override lateinit var supertypes: Collection<JavaClassifierType>
    override val methods = arrayListOf<JavaMethod>()
    override val fields = arrayListOf<JavaField>()
    override val constructors = arrayListOf<JavaConstructor>()

    override val annotationsByFqName by buildLazyValueForMap()

    private val innerClassNameToAccess: MutableMap<Name, Int> = THashMap()
    override val innerClassNames get() = innerClassNameToAccess.keys

    override val name: Name
        get() = fqName.shortName()

    override val isInterface get() = isSet(Opcodes.ACC_INTERFACE)
    override val isAnnotationType get() = isSet(Opcodes.ACC_ANNOTATION)
    override val isEnum get() = isSet(Opcodes.ACC_ENUM)
    override val lightClassOriginKind: LightClassOriginKind? get() = null

    override fun visitEnd() {
        methods.trimToSize()
        fields.trimToSize()
        constructors.trimToSize()
    }

    init {
        ClassReader(classContent ?: virtualFile.contentsToByteArray()).accept(
                this,
                ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
        )
    }

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        if (access.isSet(Opcodes.ACC_SYNTHETIC) || access.isSet(Opcodes.ACC_BRIDGE) || name == "<clinit>") return null

        // skip semi-synthetic enum methods
        if (isEnum) {
            if (name == "values" && desc.startsWith("()")) return null
            if (name == "valueOf" && desc.startsWith("(Ljava/lang/String;)")) return null
        }

        val (member, visitor) = BinaryJavaMethodBase.create(name, access, desc, signature, this, context.copyForMember(), signatureParser)

        when (member) {
            is JavaMethod -> methods.add(member)
            is JavaConstructor -> constructors.add(member)
            else -> error("Unexpected member: ${member.javaClass}")
        }

        return visitor
    }

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        if (access.isSet(Opcodes.ACC_SYNTHETIC)) return
        if (innerName == null || outerName == null) return

        if (myInternalName == outerName) {
            context.addInnerClass(name, outerName, innerName)
            innerClassNameToAccess[context.mapInternalNameToClassId(name).shortClassName] = access
        }
    }

    override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
    ) {
        this.access = this.access or access
        this.myInternalName = name

        if (signature != null) {
            parseClassSignature(signature)
        }
        else {
            this.typeParameters = emptyList()
            this.supertypes = mutableListOf<JavaClassifierType>().apply {
                addIfNotNull(superName?.convertInternalNameToClassifierType())
                interfaces?.forEach {
                    addIfNotNull(it.convertInternalNameToClassifierType())
                }
            }
        }
    }

    private fun parseClassSignature(signature: String) {
        val iterator = StringCharacterIterator(signature)
        this.typeParameters =
                signatureParser
                        .parseTypeParametersDeclaration(iterator, context)
                        .also(context::addTypeParameters)

        val supertypes = ContainerUtil.newSmartList<JavaClassifierType>()
        supertypes.addIfNotNull(signatureParser.parseClassifierRefSignature(iterator, context))
        while (iterator.current() != CharacterIterator.DONE) {
            supertypes.addIfNotNull(signatureParser.parseClassifierRefSignature(iterator, context))
        }
        this.supertypes = supertypes
    }

    private fun String.convertInternalNameToClassifierType(): JavaClassifierType =
            PlainJavaClassifierType({ context.resolveByInternalName(this) }, emptyList())

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
        if (access.isSet(Opcodes.ACC_SYNTHETIC)) return null

        val type = signatureParser.parseTypeString(StringCharacterIterator(signature ?: desc), context)

        val processedValue = processValue(value, type)

        return BinaryJavaField(Name.identifier(name), access, this, access.isSet(Opcodes.ACC_ENUM), type, processedValue).run {
            fields.add(this)

            object : FieldVisitor(ASM_API_VERSION_FOR_CLASS_READING) {
                override fun visitAnnotation(desc: String, visible: Boolean) =
                        BinaryJavaAnnotation.addAnnotation(this@run.annotations, desc, context, signatureParser)
            }
        }
    }

    /**
     * All the int-like values (including Char/Boolean) come in visitor as Integer instances
     */
    private fun processValue(value: Any?, fieldType: JavaType): Any? {
        if (fieldType !is JavaPrimitiveType || fieldType.type == null || value !is Int) return value

        return when (fieldType.type) {
             PrimitiveType.BOOLEAN -> {
                when (value) {
                    0 -> false
                    1 -> true
                    else -> value
                }
            }
            PrimitiveType.CHAR -> value.toChar()
            else -> value
        }
    }

    override fun visitAnnotation(desc: String, visible: Boolean) =
            BinaryJavaAnnotation.addAnnotation(annotations, desc, context, signatureParser)

    override fun findInnerClass(name: Name): JavaClass? {
        val access = innerClassNameToAccess[name] ?: return null

        return virtualFile.parent.findChild("${virtualFile.nameWithoutExtension}$$name.class")?.let {
            BinaryJavaClass(it, fqName.child(name), context.copyForMember(), signatureParser, access, this)
        }
    }
}
