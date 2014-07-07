/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.lazy.descriptors

import org.jetbrains.jet.lang.resolve.lazy.ResolveSession
import org.jetbrains.jet.lang.psi.JetScript
import org.jetbrains.jet.lang.resolve.lazy.LazyEntity
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorNonRootImpl
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor
import org.jetbrains.jet.lang.resolve.scopes.receivers.ScriptReceiver
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.kotlin.util.sure
import org.jetbrains.jet.lang.resolve.ScriptParameterResolver
import org.jetbrains.jet.lang.resolve.ScriptBodyResolver
import org.jetbrains.jet.lang.descriptors.impl.ScriptCodeDescriptor
import org.jetbrains.jet.lang.types.DeferredType
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil
import org.jetbrains.jet.lang.types.TypeSubstitutor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler
import org.jetbrains.jet.lang.resolve.scopes.WritableScope
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.resolve.source.toSourceElement

public class LazyScriptDescriptor(
        val resolveSession: ResolveSession,
        scriptBodyResolver: ScriptBodyResolver,
        val jetScript: JetScript,
        val _priority: Int
) : ScriptDescriptor, LazyEntity, DeclarationDescriptorNonRootImpl(
        jetScript.getContainingJetFile().getPackageFqName().let {
            fqName ->
            resolveSession.getPackageFragment(fqName).sure("Package not found $fqName")
        },
        Annotations.EMPTY,
        ScriptDescriptor.NAME,
        jetScript.toSourceElement()
 ) {
    {
        resolveSession.getTrace().record(BindingContext.SCRIPT, jetScript, this)
    }

    private val _implicitReceiver = ReceiverParameterDescriptorImpl(this, KotlinBuiltIns.getInstance().getAnyType(), ScriptReceiver(this))

    override fun getThisAsReceiverParameter() = _implicitReceiver

    override fun getPriority() = _priority

    override fun getClassDescriptor() = resolveSession.getClassDescriptorForScript(jetScript) as LazyScriptClassDescriptor

    override fun getScriptResultProperty(): PropertyDescriptor = getClassDescriptor().getScopeForMemberLookup().getScriptResultProperty()

    private val _scriptCodeDescriptor = resolveSession.getStorageManager().createLazyValue {
        val result = ScriptCodeDescriptor(this)
        result.initialize(
                _implicitReceiver,
                ScriptParameterResolver.resolveScriptParameters(jetScript, this),
                DeferredType.create(resolveSession.getStorageManager(), resolveSession.getTrace()) {
                    scriptBodyResolver.resolveScriptReturnType(jetScript, this, resolveSession.getTrace())
                }
        )
        result
    }

    override fun getScriptCodeDescriptor() = _scriptCodeDescriptor()

    override fun getScopeForBodyResolution(): JetScope {
        val parametersScope = WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING, "Parameters of " + this)
        parametersScope.setImplicitReceiver(_implicitReceiver)
        for (valueParameterDescriptor in getScriptCodeDescriptor().getValueParameters()) {
            parametersScope.addVariableDescriptor(valueParameterDescriptor)
        }
        parametersScope.changeLockLevel(WritableScope.LockLevel.READING)

        return ChainedScope(
                this,
                "Scope for body resolution for " + this,
                resolveSession.getScopeProvider().getFileScope(jetScript.getContainingJetFile()),
                parametersScope
        )
    }

    override fun forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(getClassDescriptor())
        ForceResolveUtil.forceResolveAllContents(getScriptCodeDescriptor())
    }

    override fun substitute(substitutor: TypeSubstitutor) = this

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitScriptDescriptor(this, data) as R
    }
}