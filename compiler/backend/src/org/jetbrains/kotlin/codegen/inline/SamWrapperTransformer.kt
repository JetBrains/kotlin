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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.org.objectweb.asm.*

class SamWrapperTransformationInfo(
    override val oldClassName: String,
    private val inliningContext: InliningContext,
    private val alreadyRegenerated: Boolean
) : TransformationInfo {
    override val nameGenerator: NameGenerator
        get() = object : NameGenerator("stub") {
            override fun getGeneratorClass(): String {
                error("Shouldn't be called on $oldClassName transformation")
            }

            override fun subGenerator(inliningMethod: String?): NameGenerator {
                error("Shouldn't be called on $oldClassName transformation")
            }

            override fun subGenerator(lambdaNoWhen: Boolean, nameSuffix: String?): NameGenerator {
                error("Shouldn't be called on $oldClassName transformation")
            }
        }

    //TODO consider to use package class instead of inliningContext.root.callSiteInfo.ownerClassName
    override val newClassName: String
        get() = inliningContext.root.callSiteInfo.ownerClassName + "\$inlined" + "\$sam$".run { this + oldClassName.substringAfter(this) }

    override fun shouldRegenerate(sameModule: Boolean) = !sameModule && !alreadyRegenerated

    override fun canRemoveAfterTransformation() = false

    override fun createTransformer(
        state: GenerationState, inliningContext: InliningContext, sameModule: Boolean, continuationClassName: String?,
    ): ObjectTransformer<*> = SamWrapperTransformer(state, this, inliningContext)
}

class SamWrapperTransformer(
    state: GenerationState, transformationInfo: SamWrapperTransformationInfo, private val inliningContext: InliningContext,
) : ObjectTransformer<SamWrapperTransformationInfo>(transformationInfo, state) {
    override fun doTransform(parentRemapper: FieldRemapper): InlineResult {
        val classReader = createClassReader()
        val classBuilder = createRemappingClassBuilderViaFactory(inliningContext)

        classReader.accept(object : ClassVisitor(Opcodes.API_VERSION, classBuilder.visitor) {
            private var originalVersion = 0
            private val loadableDescriptors = LinkedHashSet<String>()

            private fun addIfRecompiledKotlinValueClass(type: Type) {
                if (type.isRecompiledKotlinValueClass()) loadableDescriptors.add(type.descriptor)
            }

            override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<String>) {
                originalVersion = version
                classBuilder.defineClass(
                    regeneratedClassVersion(version, state.config.classFileVersion), access, name, signature, superName, interfaces,
                )
            }

            override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
                super.visitInnerClass(name, outerName, innerName, regeneratedInnerClassAccess(access, originalVersion, state.config.classFileVersion))
            }

            override fun visitAttribute(attribute: Attribute) {
                if (attribute is LoadableDescriptorsAttribute) loadableDescriptors.addAll(attribute.descriptors)
                else super.visitAttribute(attribute)
            }

            override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
                addIfRecompiledKotlinValueClass(Type.getType(descriptor))
                return super.visitField(access, name, descriptor, signature, value)
            }

            override fun visitMethod(
                access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?,
            ): MethodVisitor? {
                Type.getArgumentTypes(descriptor).forEach(::addIfRecompiledKotlinValueClass)
                addIfRecompiledKotlinValueClass(Type.getReturnType(descriptor))
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }

            override fun visitEnd() {
                if (loadableDescriptors.isNotEmpty()) {
                    super.visitAttribute(LoadableDescriptorsAttribute(loadableDescriptors.toList()))
                }
                super.visitEnd()
            }

        }, LOADABLE_DESCRIPTORS_ATTRIBUTE_PROTOTYPES, ClassReader.SKIP_FRAMES)
        classBuilder.done(inliningContext.config.generateSmapCopyToAnnotation)

        return transformationResult
    }
}
