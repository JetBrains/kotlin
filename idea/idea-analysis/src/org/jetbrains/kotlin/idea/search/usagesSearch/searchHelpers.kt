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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.LightClassUtil.PropertyAccessorsPsiMethods
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.idea.references.matchesTarget
import org.jetbrains.kotlin.idea.search.KOTLIN_NAMED_ARGUMENT_SEARCH_CONTEXT
import org.jetbrains.kotlin.idea.search.and
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchFilter.False
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchFilter.True
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope
import org.jetbrains.kotlin.lexer.JetSingleValueToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.effectiveDeclarations
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassReceiver
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.ArrayList
import java.util.Collections

val isTargetUsage = (PsiReference::matchesTarget).searchFilter

fun PsiNamedElement.getAccessorNames(readable: Boolean = true, writable: Boolean = true): List<String> {
    fun PropertyAccessorsPsiMethods.toNameList(): List<String> {
        val getter = getGetter()
        val setter = getSetter()

        val result = ArrayList<String>()
        if (readable && getter != null) result.add(getter.getName())
        if (writable && setter != null) result.add(setter.getName())
        return result
    }

    if (this !is JetDeclaration || JetPsiUtil.isLocal(this)) return Collections.emptyList()

    when (this) {
        is JetProperty ->
            return LightClassUtil.getLightClassPropertyMethods(this).toNameList()
        is JetParameter ->
            if (hasValOrVar()) {
                return LightClassUtil.getLightClassPropertyMethods(this).toNameList()
            }
    }

    return Collections.emptyList()
}

public fun PsiNamedElement.getClassNameForCompanionObject(): String? {
    return if (this is JetObjectDeclaration) {
        getNonStrictParentOfType<JetClass>()?.getName()
    } else {
        null
    }
}

public fun PsiNamedElement.getSpecialNamesToSearch(): List<String> {
    val name = getName()
    return when {
        name == null || !Name.isValidIdentifier(name) -> Collections.emptyList<String>()
        this is JetParameter -> {
            val componentFunctionName = this.dataClassComponentFunctionName()
            if (componentFunctionName == null) return Collections.emptyList<String>()

            return listOf(componentFunctionName.asString(), JetTokens.LPAR.getValue())
        }
        else -> Name.identifier(name).getOperationSymbolsToSearch().map { (it as JetSingleValueToken).getValue() }
    }
}

fun JetParameter.dataClassComponentFunctionName(): Name? {
    if (!hasValOrVar()) return null

    // Forcing full resolve of owner class: otherwise DATA_CLASS_COMPONENT_FUNCTION won't be calculated.
    // This is a hack, but better solution would need refactoring of data class component function resolution: they need to be resolved
    // when resolving corresponding property.
    LightClassUtil.getLightClassPropertyMethods(this)

    val context = this.analyze()
    val paramDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? ValueParameterDescriptor
    return context[BindingContext.DATA_CLASS_COMPONENT_FUNCTION, paramDescriptor]?.let {
        it.getName()
    }
}

public abstract class UsagesSearchHelper<T : PsiNamedElement> {
    protected open fun makeFilter(target: UsagesSearchTarget<T>): UsagesSearchFilter = isTargetUsage

    protected open fun makeWordList(target: UsagesSearchTarget<T>): List<String> {
        return with(target.element) {
            getName().singletonOrEmptyList() + getAccessorNames() + getClassNameForCompanionObject().singletonOrEmptyList() + getSpecialNamesToSearch()
        }
    }


    protected open fun makeItemList(target: UsagesSearchTarget<T>): List<UsagesSearchRequestItem> =
            Collections.singletonList(newItem(target))

    protected open fun makeAdditionalSearchDelegate(target: T): ((Processor<PsiReference>) -> Boolean)? = null

    fun newItem(target: UsagesSearchTarget<T>): UsagesSearchRequestItem {
        return UsagesSearchRequestItem(target, makeWordList(target), makeFilter(target), makeAdditionalSearchDelegate(target.element))
    }

    fun newRequest(target: UsagesSearchTarget<T>): UsagesSearchRequest {
        return UsagesSearchRequest(target.element.getProject(), makeItemList(target))
    }
}

val isNotImportUsage = !((PsiReference::isImportUsage).searchFilter)

interface ImportAwareSearchHelper {
    public val skipImports: Boolean

    protected val isFilteredImport: UsagesSearchFilter
        get() = isNotImportUsage.ifOrTrue(skipImports)
}

open class DefaultSearchHelper<T: PsiNamedElement>(
        override val skipImports: Boolean = false
): UsagesSearchHelper<T>(), ImportAwareSearchHelper {
    override fun makeFilter(target: UsagesSearchTarget<T>): UsagesSearchFilter = isTargetUsage and isFilteredImport
}

val isClassConstructorUsage = (PsiReference::isConstructorUsage).searchFilter

class ClassUsagesSearchHelper(
        public val constructorUsages: Boolean = false,
        public val nonConstructorUsages: Boolean = false,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetClassOrObject>(skipImports) {
    override fun makeFilter(target: UsagesSearchTarget<JetClassOrObject>): UsagesSearchFilter =
            super.makeFilter(target) and when {
                constructorUsages && !nonConstructorUsages -> isClassConstructorUsage
                !constructorUsages && nonConstructorUsages -> !isClassConstructorUsage
                !constructorUsages && !nonConstructorUsages -> False
                else -> True
            }

    override fun makeAdditionalSearchDelegate(target: JetClassOrObject): ((Processor<PsiReference>) -> Boolean)? {
        if (target is JetObjectDeclaration && target.isCompanion()) {
            val companionObjectDescriptor = target.descriptor
            val klass = target.getStrictParentOfType<JetClass>() ?: return null
            return { processor ->
                klass.acceptChildren(object : JetVisitorVoid() {
                    override fun visitJetElement(element: JetElement) {
                        if (element == target) return // skip companion object itself
                        element.acceptChildren(this)

                        val bindingContext = element.analyze()
                        val call = bindingContext[BindingContext.CALL, element] ?: return
                        val resolvedCall = bindingContext[BindingContext.RESOLVED_CALL, call] ?: return
                        if ((resolvedCall.getDispatchReceiver() as? ClassReceiver)?.getDeclarationDescriptor() == companionObjectDescriptor
                            || (resolvedCall.getExtensionReceiver() as? ClassReceiver)?.getDeclarationDescriptor() == companionObjectDescriptor) {
                            element.getReferences().forEach { processor.process(it) }
                        }
                    }
                })

                true
            }
        }
        return null
    }
}

class ClassDeclarationsUsagesSearchHelper(
        public val functionUsages: Boolean = false,
        public val propertyUsages: Boolean = false,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetClassOrObject>(skipImports) {
    override fun makeItemList(target: UsagesSearchTarget<JetClassOrObject>): List<UsagesSearchRequestItem> {
        val items = ArrayList<UsagesSearchRequestItem>()
        val declHelper = DefaultSearchHelper<JetNamedDeclaration>(skipImports)

        for (decl in target.element.effectiveDeclarations()) {
            if ((decl is JetNamedFunction && functionUsages) || ((decl is JetProperty || decl is JetParameter) && propertyUsages)) {
                items.add(declHelper.newItem(target.withTarget(decl as JetNamedDeclaration)))
            }
        }

        return items
    }
}

val isOverrideUsage = (PsiReference::isCallableOverrideUsage).searchFilter

interface OverrideSearchHelper {
    public val selfUsages: Boolean
    public val overrideUsages: Boolean

    val isTargetOrOverrideUsage: UsagesSearchFilter
        get() = isTargetUsage.ifOrFalse(selfUsages) or isOverrideUsage.ifOrFalse(overrideUsages)
}

val isOverloadUsage = (PsiReference::isUsageInContainingDeclaration).searchFilter
val isExtensionUsage = (PsiReference::isExtensionOfDeclarationClassUsage).searchFilter

class FunctionUsagesSearchHelper(
        public val overloadUsages: Boolean = false,
        public val extensionUsages: Boolean = false,
        override val selfUsages: Boolean = true,
        override val overrideUsages: Boolean = true,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetFunction>(skipImports), OverrideSearchHelper {
    override fun makeFilter(target: UsagesSearchTarget<JetFunction>): UsagesSearchFilter {
        return (isTargetOrOverrideUsage
        or isOverloadUsage.ifOrFalse(overloadUsages)
        or isExtensionUsage.ifOrFalse(extensionUsages)) and isFilteredImport
    }
}

val isPropertyReadOnlyUsage = (PsiReference::isPropertyReadOnlyUsage).searchFilter

// Used for JetProperty and JetParameter
class PropertyUsagesSearchHelper(
        public val readUsages: Boolean = true,
        public val writeUsages: Boolean = true,
        public val namedArgumentUsages: Boolean = true,
        public override val selfUsages: Boolean = true,
        public override val overrideUsages: Boolean = true,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetNamedDeclaration>(skipImports), OverrideSearchHelper {
    override fun makeItemList(target: UsagesSearchTarget<JetNamedDeclaration>): List<UsagesSearchRequestItem> {
        val element = target.element
        if (element is JetParameter) {
            val realUsagesScope = element.useScope and target.effectiveScope
            val realUsagesTarget = target.withScope(realUsagesScope)
            val realUsagesFilter = makeFilter(target) and !(PsiReference::isNamedArgumentUsage).searchFilter
            val realUsagesRequest = UsagesSearchRequestItem(realUsagesTarget, makeWordList(target), realUsagesFilter, makeAdditionalSearchDelegate(target.element))

            if (!namedArgumentUsages) {
                return listOf(realUsagesRequest)
            }

            val function = element.ownerFunction
                           ?: return listOf(realUsagesRequest) // parameter in for or something like that
            if (function.nameAsName?.isSpecial ?: true) { // function literal or anonymous function
                return listOf(realUsagesRequest)
            }

            var scope = function.useScope and target.effectiveScope
            if (scope is GlobalSearchScope) {
                scope = JetSourceFilterScope.kotlinSourcesAndLibraries(scope, element.project)
            }

            val additionalFilter1 = AdditionalFileFilter(element.name!!, KOTLIN_NAMED_ARGUMENT_SEARCH_CONTEXT, true)
            val additionalFilter2 = AdditionalFileFilter(function.name!!, UsageSearchContext.IN_CODE, true)
            val namedArgsRequest = UsagesSearchRequestItem(target.withScope(scope),
                                                           listOf(element.name!!),
                                                           (PsiReference::isNamedArgumentUsage).searchFilter and makeFilter(target),
                                                           additionalFileFilters = listOf(additionalFilter1, additionalFilter2))
            return listOf(realUsagesRequest, namedArgsRequest)
        }

        return super<DefaultSearchHelper>.makeItemList(target)
    }

    override fun makeWordList(target: UsagesSearchTarget<JetNamedDeclaration>): List<String> {
        return with(target.element) {
            ContainerUtil.createMaybeSingletonList(getName()) +
                    getAccessorNames(readable = readUsages, writable = writeUsages) +
                    getSpecialNamesToSearch()
        }
    }

    override fun makeFilter(target: UsagesSearchTarget<JetNamedDeclaration>): UsagesSearchFilter {
        val readWriteFilter = when {
            readUsages && writeUsages -> True
            readUsages -> isPropertyReadOnlyUsage
            writeUsages -> !isPropertyReadOnlyUsage
            else -> False
        }
        var result = isTargetOrOverrideUsage and readWriteFilter and isFilteredImport
        if (!namedArgumentUsages) {
           result = result and !(PsiReference::isNamedArgumentUsage).searchFilter
        }
        return result
    }

    companion object {
    }
}

private fun PsiReference.isNamedArgumentUsage(): Boolean {
    if (this !is JetSimpleNameReference) return false
    return expression.parent is JetValueArgumentName
}


