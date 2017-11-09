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

private operator fun ReferenceVariants.plus(other: ReferenceVariants): ReferenceVariants {
    return ReferenceVariants(imported.union(other.imported), notImportedExtensions.union(other.notImportedExtensions))
}

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

    private data class FilterConfiguration internal constructor(val descriptorKindFilter: DescriptorKindFilter,
                                                        val additionalPropertyNameFilter: ((String) -> Boolean)?,
                                                        val shadowedDeclarationsFilter: ShadowedDeclarationsFilter?,
                                                        val completeExtensionsFromIndices: Boolean)

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
        val config = configuration(descriptorKindFilter)

        val basic = collectBasicVariants(config)
        return basic + collectExtensionVariants(config, basic)
    }

    fun collectReferenceVariants(descriptorKindFilter: DescriptorKindFilter, consumer: (ReferenceVariants) -> Unit) {
        assert(!isCollectingFinished)
        val config = configuration(descriptorKindFilter)

        val basic = collectBasicVariants(config)
        consumer(basic)
        val extensions = collectExtensionVariants(config, basic)
        consumer(extensions)
    }

    private fun collectBasicVariants(filterConfiguration: FilterConfiguration): ReferenceVariants {
        val variants = doCollectBasicVariants(filterConfiguration)
        collectedImported += variants.imported
        return variants
    }

    private fun collectExtensionVariants(filterConfiguration: FilterConfiguration, basicVariants: ReferenceVariants): ReferenceVariants {
        val variants = doCollectExtensionVariants(filterConfiguration, basicVariants)
        collectedImported += variants.imported
        collectedNotImportedExtensions += variants.notImportedExtensions
        return variants
    }

    private fun configuration(descriptorKindFilter: DescriptorKindFilter): FilterConfiguration {
        val completeExtensionsFromIndices = descriptorKindFilter.kindMask.and(DescriptorKindFilter.CALLABLES_MASK) != 0
                                            && DescriptorKindExclude.Extensions !in descriptorKindFilter.excludes
                                            && callTypeAndReceiver !is CallTypeAndReceiver.IMPORT_DIRECTIVE
        @Suppress("NAME_SHADOWING")
        val descriptorKindFilter = if (completeExtensionsFromIndices)
            descriptorKindFilter exclude TopLevelExtensionsExclude // handled via indices
        else
            descriptorKindFilter

        val getOrSetPrefix = GET_SET_PREFIXES.firstOrNull { prefix.startsWith(it) }
        val additionalPropertyNameFilter: ((String) -> Boolean)? = getOrSetPrefix
                ?.let { prefixMatcher.cloneWithPrefix(prefix.removePrefix(getOrSetPrefix).decapitalizeSmart()).asStringNameFilter() }

        val shadowedDeclarationsFilter = if (runtimeReceiver != null)
            ShadowedDeclarationsFilter(bindingContext, resolutionFacade, nameExpression, runtimeReceiver)
        else
            ShadowedDeclarationsFilter.create(bindingContext, resolutionFacade, nameExpression, callTypeAndReceiver)

        return ReferenceVariantsCollector.FilterConfiguration(descriptorKindFilter, additionalPropertyNameFilter, shadowedDeclarationsFilter, completeExtensionsFromIndices)
    }

    private fun doCollectBasicVariants(filterConfiguration: FilterConfiguration): ReferenceVariants {
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

        val (descriptorKindFilter, additionalPropertyNameFilter) = filterConfiguration

        var basicVariants = getReferenceVariants(descriptorKindFilter, descriptorNameFilter.toNameFilter())
        if (additionalPropertyNameFilter != null) {
            basicVariants += getReferenceVariants(descriptorKindFilter.intersect(DescriptorKindFilter.VARIABLES), additionalPropertyNameFilter.toNameFilter())
            basicVariants = basicVariants.distinct()
        }
        return ReferenceVariants(filterConfiguration.filterVariants(basicVariants).toHashSet(), emptyList())
    }

    private fun doCollectExtensionVariants(filterConfiguration: FilterConfiguration, basicVariants: ReferenceVariants): ReferenceVariants {
        val (_, additionalPropertyNameFilter, shadowedDeclarationsFilter, completeExtensionsFromIndices) = filterConfiguration

        if (completeExtensionsFromIndices) {
            val nameFilter = if (additionalPropertyNameFilter != null)
                descriptorNameFilter or additionalPropertyNameFilter
            else
                descriptorNameFilter
            val extensions = if (runtimeReceiver != null)
                indicesHelper.getCallableTopLevelExtensions(callTypeAndReceiver, listOf(runtimeReceiver.type), nameFilter)
            else
                indicesHelper.getCallableTopLevelExtensions(callTypeAndReceiver, nameExpression, bindingContext, nameFilter)

            val (extensionsVariants, notImportedExtensions) = extensions.partition { importableFqNameClassifier.isImportableDescriptorImported(it) }

            val notImportedDeclarationsFilter =
                    shadowedDeclarationsFilter?.createNonImportedDeclarationsFilter<CallableDescriptor>(importedDeclarations = basicVariants.imported + extensionsVariants)

            val filteredImported = filterConfiguration.filterVariants(extensionsVariants + basicVariants.imported)

            val importedExtensionsVariants = filteredImported.filter { it !in basicVariants.imported }

            return ReferenceVariants(
                    importedExtensionsVariants,
                    notImportedExtensions.let { variants -> notImportedDeclarationsFilter?.invoke(variants) ?: variants }
            )
        }

        return ReferenceVariants(emptyList(), emptyList())
    }

    private fun <TDescriptor : DeclarationDescriptor> FilterConfiguration.filterVariants(_variants: Collection<TDescriptor>): Collection<TDescriptor> {
        var variants = _variants

        if (shadowedDeclarationsFilter != null)
            variants = shadowedDeclarationsFilter.filter(variants)

        if (!configuration.javaGettersAndSetters)
            variants = referenceVariantsHelper.filterOutJavaGettersAndSetters(variants)

        if (!configuration.dataClassComponentFunctions)
            variants = variants.filter { !isDataClassComponentFunction(it) }

        return variants
    }


    private val GET_SET_PREFIXES = listOf("get", "set", "ge", "se", "g", "s")

    private object TopLevelExtensionsExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor !is CallableMemberDescriptor) return false
            if (descriptor.extensionReceiverParameter == null) return false
            if (descriptor.kind != CallableMemberDescriptor.Kind.DECLARATION) return false /* do not filter out synthetic extensions */
            if (descriptor.isArtificialImportAliasedDescriptor) return false // do not exclude aliased descriptors - they cannot be completed via indices
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
