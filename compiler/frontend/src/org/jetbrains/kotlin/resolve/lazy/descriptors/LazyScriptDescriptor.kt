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
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.data.KtScriptInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.script.ReplResultPropertyDescriptor
import org.jetbrains.kotlin.resolve.lazy.descriptors.script.ScriptProvidedPropertiesDescriptor
import org.jetbrains.kotlin.resolve.lazy.descriptors.script.classId
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptPriorities
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
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

    val resultValue: ReplResultPropertyDescriptor? by lazy { provideResultValue() }

    private fun provideResultValue(): ReplResultPropertyDescriptor? {
        val expression = scriptInfo.script
            .getChildOfType<KtBlockExpression>()
            ?.getChildrenOfType<KtScriptInitializer>()?.lastOrNull()
            ?.getChildOfType<KtExpression>()

        val type = expression?.let {
            resolveSession.trace.bindingContext.getType(it)
        }

        return if (type != null && !type.isUnit() && !type.isNothing()) {
            resultFieldName()?.let {
                ReplResultPropertyDescriptor(
                    Name.identifier(it),
                    type,
                    this.thisAsReceiverParameter,
                    this,
                    expression.toSourceElement()
                )
            }
        } else null
    }

    fun resultFieldName(): String? {
        val scriptPriority = scriptInfo.script.getUserData(ScriptPriorities.PRIORITY_KEY)
        if (scriptPriority != null) {
            return "res$scriptPriority"
        }
        val scriptName = name.asString()
        return if (scriptName.startsWith("Line_")) {
            "res${scriptName.split("_")[1]}"
        } else "\$\$result"
    }

    private val sourceElement = scriptInfo.script.toSourceElement()

    override fun getSource() = sourceElement

    private val priority: Int = ScriptPriorities.getScriptPriority(scriptInfo.script)

    override fun getPriority() = priority

    val scriptDefinition: () -> KotlinScriptDefinition = resolveSession.storageManager.createLazyValue {
        val file = scriptInfo.script.containingKtFile
        scriptInfo.script.kotlinScriptDefinition ?: throw RuntimeException("file ${file.name} is not a script")
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

    internal val baseClassDescriptor: () -> ClassDescriptor? = resolveSession.storageManager.createNullableLazyValue {
        val template = scriptDefinition().template
        findTypeDescriptor(
            template,
            if (template.qualifiedName?.startsWith("kotlin.script.templates.standard") == true) Errors.MISSING_SCRIPT_STANDARD_TEMPLATE
            else Errors.MISSING_SCRIPT_BASE_CLASS
        )
    }

    override fun computeSupertypes() = listOf(baseClassDescriptor()?.defaultType ?: builtIns.anyType)

    private val scriptImplicitReceivers: () -> List<ClassDescriptor> = resolveSession.storageManager.createLazyValue {
        scriptDefinition().implicitReceivers.mapNotNull { receiver ->
            findTypeDescriptor(receiver, Errors.MISSING_SCRIPT_RECEIVER_CLASS)
        }
    }

    internal fun findTypeDescriptor(kClass: KClass<*>, errorDiagnostic: DiagnosticFactory1<PsiElement, String>?): ClassDescriptor? =
        findTypeDescriptor(kClass.classId, kClass.toString(), errorDiagnostic)

    internal fun findTypeDescriptor(type: KType, errorDiagnostic: DiagnosticFactory1<PsiElement, String>?): ClassDescriptor? =
        findTypeDescriptor(type.classId, type.toString(), errorDiagnostic)

    internal fun findTypeDescriptor(
        classId: ClassId?, typeName: String,
        errorDiagnostic: DiagnosticFactory1<PsiElement, String>?
    ): ClassDescriptor? {
        val typeDescriptor = classId?.let { module.findClassAcrossModuleDependencies(it) }
        if (typeDescriptor == null && errorDiagnostic != null) {
            // TODO: use PositioningStrategies to highlight some specific place in case of error, instead of treating the whole file as invalid
            resolveSession.trace.report(
                errorDiagnostic.on(
                    scriptInfo.script,
                    classId?.asSingleFqName()?.toString() ?: typeName
                )
            )
        }
        return typeDescriptor
    }

    override fun getImplicitReceivers(): List<ClassDescriptor> = scriptImplicitReceivers()

    private val scriptProvidedProperties: () -> ScriptProvidedPropertiesDescriptor = resolveSession.storageManager.createLazyValue {
        ScriptProvidedPropertiesDescriptor(this)
    }

    override fun getScriptProvidedProperties(): List<PropertyDescriptor> = scriptProvidedProperties().properties()

    private val scriptOuterScope: () -> LexicalScope = resolveSession.storageManager.createLazyValue {
        var outerScope = super.getOuterScope()
        val outerScopeReceivers = implicitReceivers.let {
            if (scriptDefinition().providedProperties.isEmpty()) {
                it
            } else {
                it + ScriptProvidedPropertiesDescriptor(this)
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

    private val scriptClassAnnotations: () -> Annotations = resolveSession.storageManager.createLazyValue {
        baseClassDescriptor()?.annotations?.let { ann ->
            FilteredAnnotations(ann) { fqname ->
                val shortName = fqname.shortName().identifier
                // TODO: consider more precise annotation filtering
                !shortName.startsWith("KotlinScript") && !shortName.startsWith("ScriptTemplate")
            }
        } ?: super.annotations
    }

    override val annotations: Annotations
        get() = scriptClassAnnotations()
}
