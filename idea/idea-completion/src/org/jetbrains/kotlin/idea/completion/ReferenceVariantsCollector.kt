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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.PrefixMatcher
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.core.ImportableFqNameClassifier
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.ShadowedDeclarationsFilter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
import java.util.*

data class ReferenceVariants(val imported: Collection<DeclarationDescriptor>, val notImportedExtensions: Collection<CallableDescriptor>)

class ReferenceVariantsCollector(
        private val referenceVariantsHelper: ReferenceVariantsHelper,
        private val indicesHelper: KotlinIndicesHelper,
        private val prefixMatcher: PrefixMatcher,
        private val nameExpression: KtSimpleNameExpression,
        private val callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        private val resolutionFacade: ResolutionFacade,
        private val bindingContext: BindingContext,
        private val importableFqNameClassifier: ImportableFqNameClassifier,
        private val configuration: CompletionSessionConfiguration,
        private val runtimeReceiver: ExpressionReceiver? = null
) {
    private val prefix = prefixMatcher.prefix
    private val descriptorNameFilter = prefixMatcher.asStringNameFilter()

    private val collectedImported = LinkedHashSet<DeclarationDescriptor>()
    private val collectedNotImportedExtensions = LinkedHashSet<CallableDescriptor>()
    private var isCollectingFinished = false

    val allCollected: ReferenceVariants
        get() {
            assert(isCollectingFinished)
            return ReferenceVariants(collectedImported, collectedNotImportedExtensions)
        }

    fun collectingFinished() {
        assert(!isCollectingFinished)
        isCollectingFinished = true
    }

    fun collectReferenceVariants(descriptorKindFilter: DescriptorKindFilter): ReferenceVariants {
        assert(!isCollectingFinished)
        val variants = doCollectReferenceVariants(descriptorKindFilter)
        collectedImported.addAll(variants.imported)
        collectedNotImportedExtensions.addAll(variants.notImportedExtensions)
        return variants
    }

    private val GET_SET_PREFIXES = listOf("get", "set", "ge", "se", "g", "s")

    private fun doCollectReferenceVariants(descriptorKindFilter: DescriptorKindFilter): ReferenceVariants {
        val completeExtensionsFromIndices = descriptorKindFilter.kindMask.and(DescriptorKindFilter.CALLABLES_MASK) != 0
                                            && DescriptorKindExclude.Extensions !in descriptorKindFilter.excludes
                                            && callTypeAndReceiver !is CallTypeAndReceiver.IMPORT_DIRECTIVE
        @Suppress("NAME_SHADOWING")
        val descriptorKindFilter = if (completeExtensionsFromIndices)
            descriptorKindFilter exclude TopLevelExtensionsExclude // handled via indices
        else
            descriptorKindFilter

        fun getReferenceVariants(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
            return referenceVariantsHelper.getReferenceVariants(
                    nameExpression,
                    kindFilter,
                    nameFilter,
                    filterOutJavaGettersAndSetters = false,
                    filterOutShadowed = false,
                    excludeNonInitializedVariable = false,
                    useReceiverType = runtimeReceiver?.type)
        }

        var variants = getReferenceVariants(descriptorKindFilter, descriptorNameFilter.toNameFilter())

        val getOrSetPrefix = GET_SET_PREFIXES.firstOrNull { prefix.startsWith(it) }
        val additionalPropertyNameFilter: ((String) -> Boolean)? = getOrSetPrefix
                ?.let { prefixMatcher.cloneWithPrefix(prefix.removePrefix(getOrSetPrefix).decapitalizeSmart()).asStringNameFilter() }
        if (additionalPropertyNameFilter != null) {
            variants += getReferenceVariants(descriptorKindFilter.intersect(DescriptorKindFilter.VARIABLES),
                                             additionalPropertyNameFilter.toNameFilter())
            variants = variants.distinct()
        }

        var notImportedExtensions: Collection<CallableDescriptor> = emptyList()
        if (completeExtensionsFromIndices) {
            val nameFilter = if (additionalPropertyNameFilter != null)
                descriptorNameFilter or additionalPropertyNameFilter
            else
                descriptorNameFilter
            val extensions = if (runtimeReceiver != null)
                indicesHelper.getCallableTopLevelExtensions(callTypeAndReceiver, listOf(runtimeReceiver.type), nameFilter)
            else
                indicesHelper.getCallableTopLevelExtensions(callTypeAndReceiver, nameExpression, bindingContext, nameFilter)

            val pair = extensions.partition { importableFqNameClassifier.isImportableDescriptorImported(it) }
            variants += pair.first
            notImportedExtensions = pair.second
        }

        val shadowedDeclarationsFilter = if (runtimeReceiver != null)
            ShadowedDeclarationsFilter(bindingContext, resolutionFacade, nameExpression, runtimeReceiver)
        else
            ShadowedDeclarationsFilter.create(bindingContext, resolutionFacade, nameExpression, callTypeAndReceiver)

        if (shadowedDeclarationsFilter != null) {
            variants = shadowedDeclarationsFilter.filter(variants)
            notImportedExtensions = shadowedDeclarationsFilter
                    .createNonImportedDeclarationsFilter<CallableDescriptor>(importedDeclarations = variants)
                    .invoke(notImportedExtensions)
        }

        if (!configuration.javaGettersAndSetters) {
            variants = referenceVariantsHelper.filterOutJavaGettersAndSetters(variants)
        }

        if (!configuration.dataClassComponentFunctions) {
            variants = variants.filter { !isDataClassComponentFunction(it) }
        }

        return ReferenceVariants(variants, notImportedExtensions)
    }

    private object TopLevelExtensionsExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor !is CallableMemberDescriptor) return false
            if (descriptor.extensionReceiverParameter == null) return false
            if (descriptor.kind != CallableMemberDescriptor.Kind.DECLARATION) return false /* do not filter out synthetic extensions */
            val containingPackage = descriptor.containingDeclaration as? PackageFragmentDescriptor ?: return false
            if (containingPackage.fqName.asString().startsWith("kotlinx.android.synthetic.")) return false // TODO: temporary solution for Android synthetic extensions
            return true
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    private fun isDataClassComponentFunction(descriptor: DeclarationDescriptor): Boolean {
        return descriptor is FunctionDescriptor &&
               descriptor.isOperator &&
               DataClassDescriptorResolver.isComponentLike(descriptor.name) &&
               descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED
    }
}
