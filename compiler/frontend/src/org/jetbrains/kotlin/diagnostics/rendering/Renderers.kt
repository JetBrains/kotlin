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

package org.jetbrains.kotlin.diagnostics.rendering

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.moduleInfo
import org.jetbrains.kotlin.analyzer.unwrapPlatform
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRenderer.Companion.DEBUG_TEXT
import org.jetbrains.kotlin.renderer.PropertyAccessorRenderingPolicy
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.IDEAPlatforms
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

object Renderers {
    private val LOG = Logger.getInstance(Renderers::class.java)

    @JvmField
    val TO_STRING = Renderer<Any> { element ->
        if (element is DeclarationDescriptor) {
            LOG.warn(
                "Diagnostic renderer TO_STRING was used to render an instance of DeclarationDescriptor.\n"
                        + "This is usually a bad idea, because descriptors' toString() includes some debug information, "
                        + "which should not be seen by the user.\nDescriptor: " + element
            )
        }
        element.toString()
    }

    @JvmField
    val NAME = Renderer<Named> { it.name.asString() }

    @JvmField
    val FQ_NAME = Renderer<MemberDescriptor> { it.fqNameSafe.asString() }

    @JvmField
    val MODULE_WITH_PLATFORM = Renderer<ModuleDescriptor> { module ->
        val platform = module.platform
        val moduleName = MODULE.render(module)
        val platformNameIfAny = if (platform == null || platform.isCommon()) "" else " for " + platform.single().platformName

        moduleName + platformNameIfAny
    }

    @JvmField
    val MODULE = Renderer<ModuleDescriptor> { module ->
        module.moduleInfo?.unwrapPlatform()?.displayedName ?: module.name.asString()
    }

    @JvmField
    val VISIBILITY = Renderer<DescriptorVisibility> {
        it.externalDisplayName
    }

    @JvmField
    val DECLARATION_NAME_WITH_KIND = Renderer<DeclarationDescriptor> {
        val name = it.name.asString()
        when (it) {
            is PackageFragmentDescriptor -> "package '$name'"
            is ClassDescriptor -> "${it.renderKind()} '$name'"
            is TypeAliasDescriptor -> "typealias '$name'"
            is TypeAliasConstructorDescriptor -> "constructor of '${it.typeAliasDescriptor.name.asString()}'"
            is ConstructorDescriptor -> "constructor of '${it.constructedClass.name.asString()}'"
            is PropertyGetterDescriptor -> "getter of property '${it.correspondingProperty.name.asString()}'"
            is PropertySetterDescriptor -> "setter of property '${it.correspondingProperty.name.asString()}'"
            is FunctionDescriptor -> "function '$name'"
            is PropertyDescriptor -> "property '$name'"
            else -> throw AssertionError("Unexpected declaration kind: $it")
        }
    }

    @JvmField
    val CAPITALIZED_DECLARATION_NAME_WITH_KIND_AND_PLATFORM = Renderer<DeclarationDescriptor> { descriptor ->
        val declarationWithNameAndKind = DECLARATION_NAME_WITH_KIND.render(descriptor)
        val withPlatform = if (descriptor is MemberDescriptor && descriptor.isActual)
            "actual $declarationWithNameAndKind"
        else
            declarationWithNameAndKind

        withPlatform.replaceFirstChar(Char::uppercaseChar)
    }


    @JvmField
    val NAME_OF_CONTAINING_DECLARATION_OR_FILE = Renderer<DeclarationDescriptor> {
        if (DescriptorUtils.isTopLevelDeclaration(it) && it is DeclarationDescriptorWithVisibility && it.visibility == DescriptorVisibilities.PRIVATE) {
            "file"
        } else {
            val containingDeclaration = it.containingDeclaration
            if (containingDeclaration is PackageFragmentDescriptor) {
                containingDeclaration.fqName.asString().wrapIntoQuotes()
            } else {
                containingDeclaration!!.name.asString().wrapIntoQuotes()
            }
        }
    }

    @JvmField
    val ELEMENT_TEXT = Renderer<PsiElement> { it.text }

    @JvmField
    val DECLARATION_NAME = Renderer<KtNamedDeclaration> { it.nameAsSafeName.asString() }

    @JvmField
    val RENDER_CLASS_OR_OBJECT = Renderer { classOrObject: KtClassOrObject ->
        val name = classOrObject.name?.let { " ${it.wrapIntoQuotes()}" } ?: ""
        when {
            classOrObject !is KtClass -> "Object$name"
            classOrObject.isInterface() -> "Interface$name"
            else -> "Class$name"
        }
    }

    @JvmField
    val RENDER_CLASS_OR_OBJECT_NAME = Renderer<ClassifierDescriptorWithTypeParameters> { it.renderKindWithName() }

    @JvmField
    val RENDER_TYPE = SmartTypeRenderer(DescriptorRenderer.FQ_NAMES_IN_TYPES.withOptions {
        parameterNamesInFunctionalTypes = false
    })

    @JvmField
    val RENDER_TYPE_WITH_ANNOTATIONS = SmartTypeRenderer(DescriptorRenderer.FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS.withOptions {
        parameterNamesInFunctionalTypes = false
    })

    @JvmField
    val TYPE_PROJECTION = Renderer<TypeProjection> { projection ->
        when {
            projection.isStarProjection -> "*"
            projection.projectionKind == Variance.INVARIANT ->
                RENDER_TYPE.render(projection.type, RenderingContext.of(projection.type))
            else ->
                "${projection.projectionKind} ${RENDER_TYPE.render(projection.type, RenderingContext.of(projection.type))}"
        }
    }

    @JvmField
    val AMBIGUOUS_CALLS = Renderer { calls: Collection<ResolvedCall<*>> ->
        val descriptors = calls.map { it.resultingDescriptor }
        renderAmbiguousDescriptors(descriptors)
    }

    @JvmField
    val COMPATIBILITY_CANDIDATE = Renderer { call: CallableDescriptor ->
        renderAmbiguousDescriptors(listOf(call))
    }

    @JvmField
    val AMBIGUOUS_CALLABLE_REFERENCES = Renderer { references: Collection<CallableDescriptor> ->
        renderAmbiguousDescriptors(references)
    }

    private fun renderAmbiguousDescriptors(descriptors: Collection<CallableDescriptor>): String {
        val context = RenderingContext.Impl(descriptors)
        return descriptors
            .sortedWith(MemberComparator.INSTANCE)
            .joinToString(separator = "\n", prefix = "\n") {
                FQ_NAMES_IN_TYPES.render(it, context)
            }
    }

    @JvmStatic
    @IDEAPluginsCompatibilityAPI(
        IDEAPlatforms._213, // maybe 211 or 212 AS also used it
        message = "Please use the CommonRenderers.commaSeparated instead",
        plugins = "Android plugin in IDEA"
    )
    fun <T> commaSeparated(itemRenderer: DiagnosticParameterRenderer<T>): DiagnosticParameterRenderer<Collection<T>> =
        CommonRenderers.commaSeparated(itemRenderer)

    @JvmField
    val CLASSES_OR_SEPARATED = Renderer<Collection<ClassDescriptor>> { descriptors ->
        buildString {
            var index = 0
            for (descriptor in descriptors) {
                append(DescriptorUtils.getFqName(descriptor).asString())
                index++
                if (index <= descriptors.size - 2) {
                    append(", ")
                } else if (index == descriptors.size - 1) {
                    append(" or ")
                }
            }
        }
    }

    private fun renderTypes(
        types: Collection<KotlinType>,
        typeRenderer: DiagnosticParameterRenderer<KotlinType>,
        context: RenderingContext
    ): String {
        return StringUtil.join(types, { typeRenderer.render(it, context) }, ", ")
    }

    @JvmField
    val RENDER_COLLECTION_OF_TYPES = ContextDependentRenderer<Collection<KotlinType>> { types, context ->
        renderTypes(types, RENDER_TYPE, context)
    }

    private fun String.wrapIntoQuotes(): String = "'$this'"

    private val WHEN_MISSING_LIMIT = 7

    private val List<WhenMissingCase>.assumesElseBranchOnly: Boolean
        get() = any { it == WhenMissingCase.Unknown || it is WhenMissingCase.ConditionTypeIsExpect }

    @JvmField
    val RENDER_WHEN_MISSING_CASES = Renderer<List<WhenMissingCase>> {
        if (!it.assumesElseBranchOnly) {
            val list = it.joinToString(", ", limit = WHEN_MISSING_LIMIT) { "'$it'" }
            val branches = if (it.size > 1) "branches" else "branch"
            "$list $branches or 'else' branch instead"
        } else {
            "'else' branch"
        }
    }

    @JvmField
    val FQ_NAMES_IN_TYPES = DescriptorRenderer.FQ_NAMES_IN_TYPES.asRenderer()

    @JvmField
    val FQ_NAMES_IN_TYPES_ANNOTATIONS_WHITELIST = DescriptorRenderer.FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS.withAnnotationsWhitelist()

    @JvmField
    val FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS = DescriptorRenderer.FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS.asRenderer()

    @JvmField
    val COMPACT = DescriptorRenderer.COMPACT.asRenderer()

    @JvmField
    val COMPACT_WITHOUT_SUPERTYPES = DescriptorRenderer.COMPACT_WITHOUT_SUPERTYPES.asRenderer()

    @JvmField
    val WITHOUT_MODIFIERS = DescriptorRenderer.withOptions {
        modifiers = emptySet()
    }.asRenderer()

    @JvmField
    val SHORT_NAMES_IN_TYPES = DescriptorRenderer.SHORT_NAMES_IN_TYPES.asRenderer()

    @JvmField
    val COMPACT_WITH_MODIFIERS = DescriptorRenderer.COMPACT_WITH_MODIFIERS.asRenderer()

    @JvmField
    val DEPRECATION_RENDERER = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.withOptions {
        withoutTypeParameters = false
        receiverAfterName = false
        propertyAccessorRenderingPolicy = PropertyAccessorRenderingPolicy.PRETTY
    }.asRenderer()

    fun renderExpressionType(type: KotlinType?, dataFlowTypes: Set<KotlinType>?): String {
        if (type == null)
            return "Type is unknown"

        if (dataFlowTypes == null)
            return DEBUG_TEXT.renderType(type)

        val typesAsString = dataFlowTypes.map { DEBUG_TEXT.renderType(it) }.toMutableSet().apply { add(DEBUG_TEXT.renderType(type)) }

        return typesAsString.sorted().joinToString(separator = " & ")
    }

    fun renderCallInfo(fqName: FqNameUnsafe?, typeCall: String) =
        buildString {
            append("fqName: ${fqName?.asString() ?: "fqName is unknown"}; ")
            append("typeCall: $typeCall")
        }
}

fun DescriptorRenderer.asRenderer() = SmartDescriptorRenderer(this)
