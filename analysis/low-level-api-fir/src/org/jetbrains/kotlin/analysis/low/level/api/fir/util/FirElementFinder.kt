/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.patchDesignationPathIfNeeded
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.ifEmpty

class FirElementFinder : FirSessionComponent {
    companion object {
        fun findClassifierWithClassId(
            firFile: FirFile,
            classId: ClassId,
        ): FirClassLikeDeclaration? = collectDesignationPath(
            firFile = firFile,
            containerClassId = classId.outerClassId,
            targetDeclarationName = classId.shortClassName,
            expectedDeclarationAcceptor = { it is FirClassLikeDeclaration },
        )?.target?.let { it as FirClassLikeDeclaration }

        fun collectDesignationPath(
            firFile: FirFile,
            declarationContainerClassId: ClassId?,
            targetMemberDeclaration: FirDeclaration,
        ): FirDesignation? = collectDesignationPath(
            firFile = firFile,
            containerClassId = declarationContainerClassId,
            targetDeclarationName = FirFileStructureNode.mappingName(targetMemberDeclaration),
            expectedDeclarationAcceptor = { it == targetMemberDeclaration },
        )

        fun findDeclaration(firFile: FirFile, nonLocalDeclaration: KtDeclaration): FirDeclaration? = collectDesignationPath(
            firFile = firFile,
            nonLocalDeclaration = nonLocalDeclaration,
        )?.declarationTarget

        fun findPathToDeclarationWithTarget(
            firFile: FirFile,
            nonLocalDeclaration: KtDeclaration,
        ): List<FirDeclaration>? = collectDesignationPath(
            firFile = firFile,
            nonLocalDeclaration = nonLocalDeclaration,
        )?.let { it.path + it.declarationTarget }

        fun collectDesignationPath(
            firFile: FirFile,
            nonLocalDeclaration: KtDeclaration,
        ): FirDesignation? = collectDesignationPath(
            firFile = firFile,
            containerClassId = nonLocalDeclaration.containingClassOrObject?.getClassId(),
            targetDeclarationName = FirFileStructureNode.mappingNameByPsi(nonLocalDeclaration),
            expectedDeclarationAcceptor = { it.psi == nonLocalDeclaration },
        )

        inline fun <reified E : FirElement> findElementIn(
            container: FirElement,
            crossinline canGoInside: (E) -> Boolean = { true },
            crossinline predicate: (E) -> Boolean,
        ): E? {
            var result: E? = null
            container.accept(object : FirVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    when {
                        result != null -> return
                        element !is E || element is FirFile -> element.acceptChildren(this)
                        predicate(element) -> result = element
                        canGoInside(element) -> element.acceptChildren(this)
                    }
                }
            })

            return result
        }

        /**
         * @see collectDesignationPath
         */
        val FirDesignation.declarationTarget: FirDeclaration get() = target as FirDeclaration

        /**
         * @return [FirDesignation] where [FirDesignation.target] is [FirDeclaration]
         *
         * @see declarationTarget
         */
        fun collectDesignationPath(
            firFile: FirFile,
            containerClassId: ClassId?,
            targetDeclarationName: Name?,
            expectedDeclarationAcceptor: (FirDeclaration) -> Boolean,
        ): FirDesignation? {
            if (containerClassId != null) {
                requireWithAttachment(!containerClassId.isLocal, { "ClassId should not be local" }) {
                    withEntry("classId", containerClassId) { it.asString() }
                }

                requireWithAttachment(
                    firFile.packageFqName == containerClassId.packageFqName,
                    { "ClassId should not be local" }
                ) {
                    withEntry("FirFile.packageName", firFile.packageFqName) { it.asString() }
                    withEntry("ClassId.packageName", containerClassId.packageFqName) { it.asString() }
                }
            }

            val additionalPathPrefix = firFile.declarations
                .singleOrNull()
                .takeIf { it is FirScript }
                ?.let(FirFileStructureNode::mappingName)

            val pathSegments = listOfNotNull(additionalPathPrefix) + containerClassId?.relativeClassName?.pathSegments().orEmpty()
            val resultPath = ArrayList<FirDeclaration>(pathSegments.size + 1)
            resultPath += firFile

            val structure = firFile.llFirSession.firElementFinder.buildRootFileStructureNode(firFile)
            val result = structure.find(
                pathSegments = pathSegments,
                resultPath = resultPath,
                targetDeclarationName = targetDeclarationName,
                expectedDeclarationAcceptor = expectedDeclarationAcceptor,
            ) ?: return null

            return FirDesignation(
                path = patchDesignationPathIfNeeded(result, resultPath).ifEmpty { emptyList() },
                target = result,
            )
        }
    }

    val cache = ContainerUtil.createConcurrentWeakKeySoftValueMap<FirFile, FirFileStructureNode>()

    fun buildRootFileStructureNode(firFile: FirFile): FirFileStructureNode = cache.getOrPut(firFile) {
        FirFileStructureNode.build(firFile)
    }
}

val FirSession.firElementFinder: FirElementFinder by FirSession.sessionComponentAccessor()

/**
 * This class represents non-local declarations from a [FirFile] in a tree-like structure.
 * Each [FirFileStructureNode] is associated with a corresponding [Name] from [FirFileStructureNode.element] by [mappingName].
 *
 * ```kotlin
 * class TopLevelClass {
 *     class NestedClass {
 *         fun method() {}
 *         val property: Int = 0
 *     }
 *
 *     fun value() {}
 *     val value: Int = 1
 * }
 *
 * fun topLevelFunction() {}
 * fun topLevelFunction(i: Int) {}
 *```
 * For this file the structure will look like:
 * ```mermaid
 * graph LR
 *     File([File]) --> 'TopLevelClass'
 *     File --> 'topLevelFunction'
 *     'TopLevelClass' --> TopLevelClass(["class TopLevelClass"])
 *     'topLevelFunction' --> topLevelFunction_0(["fun topLevelFunction()"])
 *     'topLevelFunction' --> topLevelFunction_1(["fun topLevelFunction(i: Int)"])
 *     TopLevelClass --> 'NestedClass'
 *     TopLevelClass --> 'value'
 *     TopLevelClass --> 'TopLevelClass_cons'("'#60;init#62;'")
 *     'NestedClass' --> NestedClass(["class NestedClass"])
 *     'TopLevelClass_cons' --> TopLevelClass_cons(["constructor()"])
 *     'value' --> value_fun(["fun value()"])
 *     'value' --> value_prop(["val value"])
 *     NestedClass --> 'NestedClass_cons'("'#60;init#62;'")
 *     NestedClass --> 'method'
 *     'NestedClass_cons' --> NestedClass_cons(["constructor()"])
 *     'method' --> method(["fun method()"])
 * ```
 *
 * @see build
 * @see mappingName
 * @see FirElementFinder
 */
sealed class FirFileStructureNode(val element: FirDeclaration) {
    /**
     * Represents a [FirDeclaration] which can have non-local nested declarations.
     * Currently, it is [FirFile], [FirScript] and [FirRegularClass].
     *
     * @param element a container declaration.
     * @param elements nested [FirFileStructureNode] nodes based on the [element] directly nested declarations grouped by [mappingName].
     */
    class Container(element: FirDeclaration, val elements: Map<Name, List<FirFileStructureNode>>) : FirFileStructureNode(element)

    /**
     * Represents a [FirDeclaration] which cannot have non-local nested declarations.
     */
    class Leaf(element: FirDeclaration) : FirFileStructureNode(element)

    /**
     * ```kotlin
     * // FILE: main.kt
     * package pack
     *
     * class TopLevel {
     *   class Nested {
     *     fun method() {}
     *   }
     * }
     * ```
     *
     * [pathSegments] examples:
     * - `method`: `listOf(TopLevel, Nested)`
     * - `Nested`: `listOf(TopLevel)`
     *
     * @param pathSegments a path to a target declaration.
     *   It must contain only [FirScript] and/or [FirRegularClass] classes.
     *   The target declaration and the [FirFile] is not included.
     *
     * @param resultPath a list into which a path to a target declaration will be added.
     * @param targetDeclarationName the [mappingName] of a target declaration. It helps to perform the search more efficiently if present.
     * @param expectedDeclarationAcceptor a predicate that will be called on potential target declaration.
     *   It should return **true** for the expected target declaration.
     *
     * @return a target declaration if found
     */
    fun find(
        pathSegments: List<Name>,
        resultPath: MutableList<FirDeclaration>,
        targetDeclarationName: Name?,
        expectedDeclarationAcceptor: (FirDeclaration) -> Boolean,
    ): FirDeclaration? = find(
        pathSegments = pathSegments,
        pathIndex = 0,
        resultPath = resultPath,
        targetDeclarationName = targetDeclarationName,
        expectedDeclarationAcceptor = expectedDeclarationAcceptor,
    )

    /**
     * assigned index:           0              1                    2                    [targetDeclarationName]/[expectedDeclarationAcceptor]
     * result path: [FirFile] -> [FirScript] -> [FirRegularClass] -> [FirRegularClass] -> [FirDeclaration]
     * path index:  0            1              2                    3                    4
     */
    fun find(
        pathSegments: List<Name>,
        pathIndex: Int,
        resultPath: MutableList<FirDeclaration>,
        targetDeclarationName: Name?,
        expectedDeclarationAcceptor: (FirDeclaration) -> Boolean,
    ): FirDeclaration? {
        if (this !is Container) return null

        val nextSegmentName = pathSegments.getOrNull(pathIndex)
        if (nextSegmentName != null) {
            val structures = elements[nextSegmentName] ?: return null

            for (structure in structures) {
                resultPath += structure.element
                val result = structure.find(
                    pathSegments = pathSegments,
                    pathIndex = pathIndex + 1,
                    resultPath = resultPath,
                    targetDeclarationName = targetDeclarationName,
                    expectedDeclarationAcceptor = expectedDeclarationAcceptor,
                )

                if (result != null) {
                    return result
                }

                resultPath.removeLast()
            }

            // A corner case for scripts as they always present in [pathSegments] even if it is a target,
            // so it should be checked
            if (pathIndex != 0 || structures.singleOrNull()?.element !is FirScript) {
                return null
            }
        }

        return if (targetDeclarationName != null) {
            val structures = elements[targetDeclarationName].orEmpty()
            structures.firstNotNullOfOrNull {
                it.element.takeIf(expectedDeclarationAcceptor)
            }
        } else {
            elements.values.firstNotNullOfOrNull { structures ->
                structures.firstNotNullOfOrNull {
                    it.element.takeIf(expectedDeclarationAcceptor)
                }
            }
        }
    }

    companion object {
        fun build(element: FirDeclaration): FirFileStructureNode = when (element) {
            is FirFile -> Container(
                element = element,
                elements = convertDeclarations(element.declarations),
            )

            is FirScript -> Container(
                element = element,
                elements = linkedMapOf<Name, MutableList<FirFileStructureNode>>().apply {
                    convertDeclarations(element.parameters, this)
                    convertDeclarations(element.declarations, this)
                }
            )

            is FirRegularClass -> Container(
                element = element,
                elements = convertDeclarations(element.declarations),
            )

            else -> Leaf(element)
        }

        /**
         * [LinkedHashMap] is used to preserve the original declarations order.
         */
        fun convertDeclarations(
            declarations: List<FirDeclaration>,
            destination: LinkedHashMap<Name, MutableList<FirFileStructureNode>> = linkedMapOf(),
        ): Map<Name, List<FirFileStructureNode>> = declarations.groupByTo(
            destination,
            keySelector = ::mappingName,
            valueTransform = ::build,
        )

        /**
         * @see mappingNameByPsi
         */
        fun mappingName(declaration: FirDeclaration): Name = when (declaration) {
            is FirScript -> declaration.name
            is FirRegularClass -> declaration.name
            is FirSimpleFunction -> declaration.name
            is FirVariable -> declaration.name
            is FirConstructor -> SpecialNames.INIT
            is FirAnonymousInitializer -> SpecialNames.ANONYMOUS
            is FirTypeAlias -> declaration.name
            is FirCodeFragment, is FirDanglingModifierList -> SpecialNames.NO_NAME_PROVIDED

            is FirFile,
            is FirAnonymousFunction,
            is FirErrorFunction,
            is FirPropertyAccessor,
            is FirAnonymousObject,
            is FirReceiverParameter,
            is FirReplSnippet,
            is FirTypeParameter,
                -> errorWithFirSpecificEntries("Unexpected declaration ${declaration::class.simpleName}", fir = declaration)
        }

        /**
         * This implementation must be in sync with the [mappingName].
         *
         * It may return `null` if there is no fast way to get the correct name.
         *
         * It is based on [org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder], but [mappingName] may rewrite/simplify some rules.
         */
        fun mappingNameByPsi(declaration: KtDeclaration): Name? = when (declaration) {
            is KtConstructor<*> -> SpecialNames.INIT

            // Script initializers cannot be mapped as the last initializer can be transformed to a result property with an arbitrary name
            is KtClassInitializer -> SpecialNames.ANONYMOUS
            is KtCodeFragment -> SpecialNames.NO_NAME_PROVIDED
            is KtClassOrObject, is KtTypeAlias, is KtNamedFunction, is KtProperty -> declaration.nameAsSafeName
            else -> null
        }
    }
}
