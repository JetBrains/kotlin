/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.context

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.FieldInfo
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.lazy.descriptors.script.ScriptEnvironmentDescriptor
import org.jetbrains.org.objectweb.asm.Type
import kotlin.reflect.KClass

class ScriptContext(
    val typeMapper: KotlinTypeMapper,
    val scriptDescriptor: ScriptDescriptor,
    val earlierScripts: List<ScriptDescriptor>,
    contextDescriptor: ClassDescriptor,
    parentContext: CodegenContext<*>?
) : ClassContext(typeMapper, contextDescriptor, OwnerKind.IMPLEMENTATION, parentContext, null) {
    val lastStatement: KtExpression?

    val resultFieldInfo: FieldInfo
        get() {
            assert(state.replSpecific.shouldGenerateScriptResultValue) { "Should not be called unless 'scriptResultFieldName' is set" }
            val scriptResultFieldName = state.replSpecific.scriptResultFieldName!!
            return FieldInfo.createForHiddenField(state.typeMapper.mapClass(scriptDescriptor), AsmTypes.OBJECT_TYPE, scriptResultFieldName)
        }

    val script = DescriptorToSourceUtils.getSourceFromDescriptor(scriptDescriptor) as KtScript?
            ?: error("Declaration should be present for script: $scriptDescriptor")

    init {
        val lastDeclaration = script.declarations.lastOrNull()
        if (lastDeclaration is KtAnonymousInitializer) {
            this.lastStatement = lastDeclaration.body
        } else {
            this.lastStatement = null
        }
    }

    val ctorValueParametersStart = if (earlierScripts.isNotEmpty()) 1 else 0

    private val ctorImplicitReceiversParametersStart =
        ctorValueParametersStart + (scriptDescriptor.getSuperClassNotAny()?.unsubstitutedPrimaryConstructor?.valueParameters?.size ?: 0)

    private val ctorEnvironmentVarsParametersStart =
        ctorImplicitReceiversParametersStart + scriptDescriptor.implicitReceivers.size

    fun getImplicitReceiverName(index: Int): String =
        scriptDescriptor.unsubstitutedPrimaryConstructor.valueParameters[ctorImplicitReceiversParametersStart + index].name.identifier

    fun getImplicitReceiverType(index: Int): Type? {
        val receivers = script.kotlinScriptDefinition.implicitReceivers
        val kClass = receivers.getOrNull(index)?.classifier as? KClass<*>
        return kClass?.java?.classId?.let(AsmUtil::asmTypeByClassId)
    }

    fun getEnvironmentVarName(index: Int): String =
        scriptDescriptor.unsubstitutedPrimaryConstructor.valueParameters[ctorEnvironmentVarsParametersStart + index].name.identifier

    fun getEnvironmentVarType(index: Int): Type = typeMapper.mapType(scriptDescriptor.scriptEnvironmentProperties[index].type)

    fun getOuterReceiverExpression(prefix: StackValue?, thisOrOuterClass: ClassDescriptor): StackValue {
        if (thisOrOuterClass is ScriptEnvironmentDescriptor) {
            return prefix ?: StackValue.LOCAL_0
        }
        receiverDescriptors.forEachIndexed { index, outerReceiver ->
            if (outerReceiver == thisOrOuterClass) {
                return getImplicitReceiverType(index)?.let { type ->
                    val owner = typeMapper.mapType(scriptDescriptor)
                    StackValue.field(type, owner, getImplicitReceiverName(index), false, prefix ?: StackValue.LOCAL_0)
                } ?: error("Invalid script receiver: ${thisOrOuterClass.fqNameSafe}")
            }
        }
        error("Script receiver not found: ${thisOrOuterClass.fqNameSafe}")
    }

    val receiverDescriptors: List<ClassDescriptor>
        get() = scriptDescriptor.implicitReceivers

    fun getScriptFieldName(scriptDescriptor: ScriptDescriptor): String {
        val index = earlierScripts.indexOf(scriptDescriptor)
        if (index < 0) {
            throw IllegalStateException("Unregistered script: $scriptDescriptor")
        }
        return "script$" + (index + 1)
    }

    override fun toString(): String {
        return "Script: " + contextDescriptor.name.asString()
    }
}

private val Class<*>.classId: ClassId
    get() = enclosingClass?.classId?.createNestedClassId(Name.identifier(simpleName)) ?: ClassId.topLevel(FqName(name))
