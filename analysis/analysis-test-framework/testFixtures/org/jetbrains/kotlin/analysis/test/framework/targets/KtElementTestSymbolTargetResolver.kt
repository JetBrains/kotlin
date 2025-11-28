/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner

/**
 * A [KtElement] [TestSymbolTargetResolver] which resolves *all* possible [KtElement]s for a [TestSymbolTarget]. The resolver may return
 * multiple ambiguous elements. This is by design.
 *
 * The resolver can only return [KtElement]s from binary libraries when [KotlinDeserializedDeclarationsOrigin.STUBS][org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.STUBS]
 * is enabled. This is because the declaration provider's indices only contain [KtElement]s from binary libraries that we index as stubs.
 * Hence, this approach doesn't work in the Standalone test mode because it doesn't index binary libraries as stubs. Currently, the resolver
 * is only needed for LL FIR tests which don't run in that mode (in contrast to the Analysis API surface tests). In the future, we can
 * extend the resolver to also support [KtElement]s from non-indexed files. To avoid usage pitfalls, the resolver requires the `STUBS` mode
 * to be enabled. (See KT-76818.)
 *
 * The resolver currently doesn't support [PackageTarget], [EnumEntryInitializerTarget], and [SamConstructorTarget].
 */
internal class KtElementTestSymbolTargetResolver(project: Project) : TestSymbolTargetResolver<KtElement>() {
    private val declarationProvider = project.createDeclarationProvider(GlobalSearchScope.allScope(project), contextualModule = null)

    init {
        require(KotlinPlatformSettings.getInstance(project).deserializedDeclarationsOrigin == KotlinDeserializedDeclarationsOrigin.STUBS) {
            "`${KtElementTestSymbolTargetResolver::class.simpleName}` currently relies on stub-indexed binary libraries. See the class's" +
                    " KDoc for more information."
        }
    }

    override fun resolveClassTarget(target: ClassTarget): List<KtElement> = resolveClasses(target.classId)

    private fun resolveClasses(classId: ClassId): List<KtClassOrObject> =
        declarationProvider.getAllClassesByClassId(classId)
            .ifEmpty { error("Cannot find a class `$classId`.") }
            .toList()

    override fun resolveScriptTarget(target: ScriptTarget): List<KtElement> {
        val ktScript = target.file.script ?: error("The file `${target.file.name}` is not a script.")
        return listOf(ktScript)
    }

    override fun resolveTypeAliasTarget(target: TypeAliasTarget): List<KtElement> =
        declarationProvider.getAllTypeAliasesByClassId(target.classId)
            .ifEmpty { error("Cannot find a type alias `${target.classId}`.") }
            .toList()

    override fun resolveCallableTarget(target: CallableTarget): List<KtElement> {
        val callableId = target.callableId
        val classId = callableId.classId

        val callables = if (classId == null) {
            buildList<KtElement> {
                addAll(declarationProvider.getTopLevelFunctions(callableId))
                addAll(declarationProvider.getTopLevelProperties(callableId))
            }
        } else {
            // We want to find all callable elements which match the callable ID, not just the callables of the class that the symbol
            // provider would find, so we have to look into all possible class declarations.
            val classes = resolveClasses(classId)

            val callableName = callableId.callableName.asString()
            buildList {
                for (ktClass in classes) {
                    for (member in ktClass.declarations) {
                        if ((member is KtCallableDeclaration || member is KtEnumEntry) && member.name == callableName) {
                            add(member)
                        }
                    }
                }
            }
        }

        return callables.ifEmpty { error("Cannot find a callable `$callableId`.") }
    }

    override fun resolveTypeParameterTarget(target: TypeParameterTarget, owner: KtElement): KtElement? {
        requireSpecificOwner<KtTypeParameterListOwner>(target, owner)

        return owner.typeParameters.find { it.name == target.name.asString() }
    }

    override fun resolveValueParameterTarget(target: ValueParameterTarget, owner: KtElement): KtElement? {
        requireSpecificOwner<KtCallableDeclaration>(target, owner)

        return owner.valueParameters.find { it.name == target.name.asString() }
    }

    override fun resolveFieldTarget(target: FieldTarget): KtElement? {
        val callables = resolveCallableTarget(CallableTarget(target.callableId))
        return callables
            .filterIsInstance<KtProperty>()
            .singleOrNull()
            ?.fieldDeclaration
    }
}
