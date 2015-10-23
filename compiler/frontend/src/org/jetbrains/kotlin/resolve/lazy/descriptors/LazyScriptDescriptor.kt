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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorNonRootImpl
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ScriptCodeDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.parsing.JetScriptDefinitionProvider
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ScriptBodyResolver
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.LazyEntity
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.resolve.scopes.receivers.ScriptReceiver
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.sure

public class LazyScriptDescriptor(
        private val resolveSession: ResolveSession,
        scriptBodyResolver: ScriptBodyResolver,
        private val jetScript: KtScript,
        private val priority: Int
) : ScriptDescriptor, LazyEntity, DeclarationDescriptorNonRootImpl(
        jetScript.getContainingJetFile().getPackageFqName().let {
            fqName ->
            resolveSession.getPackageFragment(fqName).sure { "Package not found $fqName" }
        },
        Annotations.EMPTY,
        ScriptDescriptor.NAME,
        jetScript.toSourceElement()
) {
    init {
        resolveSession.trace.record(BindingContext.SCRIPT, jetScript, this)
    }

    private val implicitReceiver = ReceiverParameterDescriptorImpl(this, ScriptReceiver(this))

    override fun getThisAsReceiverParameter() = implicitReceiver

    override fun getPriority() = priority

    override fun getClassDescriptor() = resolveSession.getClassDescriptorForScript(jetScript) as LazyScriptClassDescriptor

    override fun getScriptResultProperty(): PropertyDescriptor = getClassDescriptor().getUnsubstitutedMemberScope().getScriptResultProperty()

    private val scriptCodeDescriptor = resolveSession.storageManager.createLazyValue {
        val result = ScriptCodeDescriptor(this)

        val file = jetScript.getContainingJetFile()
        val scriptDefinition = JetScriptDefinitionProvider.getInstance(file.getProject()).findScriptDefinition(file)

        result.initialize(
                implicitReceiver,
                scriptDefinition.getScriptParameters().mapIndexed { index, scriptParameter ->
                    ValueParameterDescriptorImpl(
                            result, null, index, Annotations.EMPTY, scriptParameter.getName(), scriptParameter.getType(),
                            /* declaresDefaultValue = */ false,
                            /* isCrossinline = */ false,
                            /* isNoinline = */ false,
                            null, SourceElement.NO_SOURCE
                    )
                },
                DeferredType.create(resolveSession.storageManager, resolveSession.trace) {
                    scriptBodyResolver.resolveScriptReturnType(jetScript, this, resolveSession.trace)
                }
        )
        result
    }

    override fun getScriptCodeDescriptor() = scriptCodeDescriptor()

    override fun getScopeForBodyResolution(): LexicalScope {
        return LexicalScopeImpl(resolveSession.fileScopeProvider.getFileScopeChain(jetScript.getContainingJetFile()),
                                this, false, implicitReceiver, "Scope for body resolution for " + this) {
            for (valueParameterDescriptor in getScriptCodeDescriptor().valueParameters) {
                addVariableDescriptor(valueParameterDescriptor)
            }
        }
    }

    override fun forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(getClassDescriptor())
        ForceResolveUtil.forceResolveAllContents(getScriptCodeDescriptor())
    }

    override fun substitute(substitutor: TypeSubstitutor) = this

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitScriptDescriptor(this, data)
    }
}
