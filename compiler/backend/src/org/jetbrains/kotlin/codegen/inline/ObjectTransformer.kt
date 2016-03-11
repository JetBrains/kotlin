/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Type

abstract class ObjectTransformer<T : TransformationInfo>(val transformationInfo: T, val state: GenerationState) {

    abstract fun doTransform(info: T, parentRemapper: FieldRemapper): InlineResult

    @JvmField
    val transformationResult = InlineResult.create()

    protected fun createRemappingClassBuilderViaFactory(inliningContext: InliningContext): ClassBuilder {
        val classBuilder = state.factory.newVisitor(
                JvmDeclarationOrigin.NO_ORIGIN,
                Type.getObjectType(transformationInfo.newClassName),
                inliningContext.root.callElement.containingFile
        )

        return RemappingClassBuilder(
                classBuilder,
                AsmTypeRemapper(inliningContext.typeRemapper, inliningContext.root.typeParameterMappings == null, transformationResult))
    }


    fun createClassReader(): ClassReader {
        return InlineCodegenUtil.buildClassReaderByInternalName(state, transformationInfo.oldClassName)
    }

}

class WhenMappingTransformer(
        whenObjectRegenerationInfo: WhenMappingTransformationInfo,
        state: GenerationState,
        val inliningContext: InliningContext
) : ObjectTransformer<WhenMappingTransformationInfo>(whenObjectRegenerationInfo, state) {

    override fun doTransform(info: WhenMappingTransformationInfo, parentRemapper: FieldRemapper): InlineResult {
        val classReader = createClassReader()
        //TODO add additional check that class is when mapping

        val classBuilder = createRemappingClassBuilderViaFactory(inliningContext)
        classReader.accept(object : ClassVisitor(InlineCodegenUtil.API, classBuilder.visitor) {
            override fun visit(version: Int, access: Int, name: String, signature: String, superName: String, interfaces: Array<String>) {
                InlineCodegenUtil.assertVersionNotGreaterThanJava6(version, name)
                super.visit(version, access, name, signature, superName, interfaces)
            }
        }, ClassReader.SKIP_FRAMES)

        return transformationResult
    }
}

