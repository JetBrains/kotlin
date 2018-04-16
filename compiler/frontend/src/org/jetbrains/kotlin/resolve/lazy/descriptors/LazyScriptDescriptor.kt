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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.data.KtScriptInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptHelper
import org.jetbrains.kotlin.script.ScriptPriorities
import org.jetbrains.kotlin.script.getScriptDefinition
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.ifEmpty
import kotlin.reflect.KClass
import kotlin.reflect.KType

class LazyScriptDescriptor(
    val resolveSession: ResolveSession,
    containingDeclaration: DeclarationDescriptor,
    name: Name,
    internal val scriptInfo: KtScriptInfo
) : ScriptDescriptor, LazyClassDescriptor(
    resolveSession,
    containingDeclaration,
    name,
    scriptInfo,
    /* isExternal = */ false
) {
    init {
        resolveSession.trace.record(BindingContext.SCRIPT, scriptInfo.script, this)
    }

    private val sourceElement = scriptInfo.script.toSourceElement()

    override fun getSource() = sourceElement

    private val priority: Int = ScriptPriorities.getScriptPriority(scriptInfo.script)

    override fun getPriority() = priority

    val scriptDefinition: KotlinScriptDefinition
            by lazy {
                val file = scriptInfo.script.containingKtFile
                getScriptDefinition(file) ?: throw RuntimeException("file ${file.name} is not a script")
            }

    override fun substitute(substitutor: TypeSubstitutor) = this

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
        visitor.visitScriptDescriptor(this, data)

    override fun createMemberScope(c: LazyClassContext, declarationProvider: ClassMemberDeclarationProvider): LazyScriptClassMemberScope =
        LazyScriptClassMemberScope(
            // Must be a ResolveSession for scripts
            c as ResolveSession,
            declarationProvider,
            this,
            c.trace
        )

    override fun getUnsubstitutedPrimaryConstructor() = super.getUnsubstitutedPrimaryConstructor()!!

    override fun computeSupertypes() =
        listOf(ScriptHelper.getInstance().getKotlinType(this, scriptDefinition.template)).ifEmpty { listOf(builtIns.anyType) }

    private val scriptImplicitReceivers: () -> List<ClassDescriptor> = resolveSession.storageManager.createLazyValue {
        scriptDefinition.implicitReceivers.mapNotNull { receiver ->
            findTypeDescriptor(receiver, Errors.MISSING_SCRIPT_RECEIVER_CLASS)
        }
    }

    private fun findTypeDescriptor(type: KType, errorDiagnostic: DiagnosticFactory1<PsiElement, FqName>): ClassDescriptor? {
        val receiverClassId = type.classifier?.let { it as? KClass<*> }?.classId
        return receiverClassId?.let {
            module.findClassAcrossModuleDependencies(it)
        } ?: also {
            // TODO: use PositioningStrategies to highlight some specific place in case of error, instead of treating the whole file as invalid
            resolveSession.trace.report(
                errorDiagnostic.on(
                    scriptInfo.script,
                    receiverClassId?.asSingleFqName() ?: FqName(type.toString())
                )
            )
        }
    }

    override fun getImplicitReceivers(): List<ClassDescriptor> = scriptImplicitReceivers()

    private val scriptEnvironmentProperties: () -> List<Pair<String, ClassDescriptor>> = resolveSession.storageManager.createLazyValue {
        scriptDefinition.environmentVariables.mapNotNull { (name, type) ->
            findTypeDescriptor(type, Errors.MISSING_SCRIPT_ENVIRONMENT_PROPERTY_CLASS)?.let {
                name to it
            }
        }
    }

    fun getScriptEnvironmentProperties(): List<Pair<String, ClassDescriptor>> = scriptEnvironmentProperties()

    private val scriptOuterScope: () -> LexicalScope = resolveSession.storageManager.createLazyValue {
        var outerScope = super.getOuterScope()
        val outerScopeReceivers = implicitReceivers.let {
            if (scriptDefinition.environmentVariables.isEmpty()) {
                it
            } else {
                it + ScriptEnvironmentDescriptor(this)
            }
        }
        for (receiverClassDescriptor in outerScopeReceivers.asReversed()) {
            outerScope = LexicalScopeImpl(
                outerScope,
                receiverClassDescriptor,
                true,
                receiverClassDescriptor.thisAsReceiverParameter,
                LexicalScopeKind.CLASS_MEMBER_SCOPE
            )
        }
        outerScope
    }

    override fun getOuterScope(): LexicalScope = scriptOuterScope()
}

private val KClass<*>.classId: ClassId
    get() = this.java.enclosingClass?.kotlin?.classId?.createNestedClassId(Name.identifier(simpleName!!))
            ?: ClassId.topLevel(FqName(qualifiedName!!))

private val KType.classId: ClassId?
    get() = classifier?.let { it as? KClass<*> }?.classId

private class ScriptEnvironmentDescriptor(script: LazyScriptDescriptor) :
    MutableClassDescriptor(
        script, ClassKind.CLASS, false, false,
        Name.special("<synthetic script environment for ${script.name}>"), SourceElement.NO_SOURCE, LockBasedStorageManager.NO_LOCKS
    ) {

    init {
        modality = Modality.FINAL
        visibility = Visibilities.PUBLIC
        setTypeParameterDescriptors(emptyList())
        createTypeConstructor()
    }

    private val memberScope by lazy {
        ScriptEnvironmentMemberScope(
            script.name.identifier,
            script.getScriptEnvironmentProperties().map {
                makeEnvironmentPropertyDescriptor(
                    Name.identifier(it.first),
                    it.second,
                    true,
                    script
                )
            }
        )
    }

    override fun getUnsubstitutedMemberScope(): MemberScope = memberScope
}

private class ScriptEnvironmentMemberScope(
    private val scriptId: String,
    private val environmentProperties: List<PropertyDescriptorImpl>
) : MemberScopeImpl() {
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> =
        environmentProperties.filter { kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK) && nameFilter(it.name) }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        environmentProperties.filter { it.name == name }

    override fun printScopeStructure(p: Printer) {
        p.println("Scope of script environment: $scriptId")
    }
}

private fun ScriptEnvironmentDescriptor.makeEnvironmentPropertyDescriptor(name: Name, typeDescriptor: ClassDescriptor, isVar: Boolean, script: LazyScriptDescriptor) =
    PropertyDescriptorImpl.create(
        script.containingDeclaration,
        Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC,
        isVar,
        name,
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        SourceElement.NO_SOURCE,
        /* lateInit = */ false, /* isConst = */ false, /* isExpect = */ false, /* isActual = */ false, /* isExternal = */ false,
        /* isDelegated = */ true
    ).also {
        it.setType(typeDescriptor.defaultType, emptyList<TypeParameterDescriptorImpl>(), thisAsReceiverParameter, null as KotlinType?)
        it.initialize(
            it.makePropertyGetterDescriptor(),
            if (!isVar) null else it.makePropertySetterDescriptor()
        )
    }

private fun PropertyDescriptorImpl.makePropertyGetterDescriptor() =
    PropertyGetterDescriptorImpl(
        this, Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC,
        /* isDefault = */ false, /* isExternal = */ false, /* isInline = */ false,
        CallableMemberDescriptor.Kind.SYNTHESIZED, null, SourceElement.NO_SOURCE
    ).also {
        it.initialize(returnType)
    }

private fun PropertyDescriptorImpl.makePropertySetterDescriptor() =
    PropertySetterDescriptorImpl(
        this, Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC,
        /* isDefault = */ false, /* isExternal = */ false, /* isInline = */ false,
        CallableMemberDescriptor.Kind.SYNTHESIZED, null, SourceElement.NO_SOURCE
    ).also {
        it.initialize(
            ValueParameterDescriptorImpl(
                this, null, 0, Annotations.EMPTY, Name.special("<set-?>"), returnType,
                /* declaresDefaultValue = */ false, /* isCrossinline = */ false, /* isNoinline = */ false,
                null, SourceElement.NO_SOURCE
            )
        )
    }
