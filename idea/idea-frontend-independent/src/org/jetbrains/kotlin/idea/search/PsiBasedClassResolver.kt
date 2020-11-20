/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.ImpreciseResolveResult
import org.jetbrains.kotlin.asJava.ImpreciseResolveResult.*
import org.jetbrains.kotlin.idea.caches.trackers.KotlinCodeBlockModificationListener
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.findTypeAliasByShortName
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.getDefaultImports
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import java.util.concurrent.atomic.AtomicInteger

/**
 * Can quickly check whether a short name reference in a given file can resolve to the class/interface/type alias
 * with the given qualified name.
 */
class PsiBasedClassResolver @TestOnly constructor(private val targetClassFqName: String) {

    private val targetShortName = targetClassFqName.substringAfterLast('.')
    private val targetPackage = targetClassFqName.substringBeforeLast('.', "")

    /**
     * Qualified names of packages which contain classes with the same short name as the target class.
     */
    private val conflictingPackages = mutableListOf<String>()

    /**
     * Qualified names of packages which contain typealiases with the same short name as the target class
     * (which may or may not resolve to the target class).
     */
    private val packagesWithTypeAliases = mutableListOf<String>()
    private var forceAmbiguity: Boolean = false
    private var forceAmbiguityForInnerAnnotations: Boolean = false
    private var forceAmbiguityForNonAnnotations: Boolean = false

    companion object {
        @get:TestOnly
        val attempts = AtomicInteger()

        @get:TestOnly
        val trueHits = AtomicInteger()

        @get:TestOnly
        val falseHits = AtomicInteger()

        private val PSI_BASED_CLASS_RESOLVER_KEY = Key<CachedValue<PsiBasedClassResolver>>("PsiBasedClassResolver")

        fun getInstance(target: PsiClass): PsiBasedClassResolver {
            target.getUserData(PSI_BASED_CLASS_RESOLVER_KEY)?.let { return it.value }

            val cachedValue = CachedValuesManager.getManager(target.project).createCachedValue(
                {
                    CachedValueProvider.Result(
                        PsiBasedClassResolver(target),
                        KotlinCodeBlockModificationListener.getInstance(target.project).kotlinOutOfCodeBlockTracker
                    )
                }, false
            )

            target.putUserData(PSI_BASED_CLASS_RESOLVER_KEY, cachedValue)

            return cachedValue.value
        }
    }

    private constructor(target: PsiClass) : this(target.qualifiedName ?: "") {
        if (target.qualifiedName == null || target.containingClass != null || targetPackage.isEmpty()) {
            forceAmbiguity = true
            return
        }

        runReadAction {
            findPotentialClassConflicts(target)
            findPotentialTypeAliasConflicts(target)
        }
    }

    private fun findPotentialClassConflicts(target: PsiClass) {
        val candidates = PsiShortNamesCache.getInstance(target.project).getClassesByName(targetShortName, target.project.allScope())
        for (candidate in candidates) {
            // An inner class can be referenced by short name in subclasses without an explicit import
            if (candidate.containingClass != null && !candidate.hasModifierProperty(PsiModifier.PRIVATE)) {
                if (candidate.isAnnotationType) {
                    forceAmbiguityForInnerAnnotations = true
                } else {
                    forceAmbiguityForNonAnnotations = true
                }
                break
            }

            if (candidate.qualifiedName == target.qualifiedName) {
                // File with same FQ name in another module, don't bother with analyzing dependencies
                if (candidate.navigationElement.containingFile != target.navigationElement.containingFile) {
                    forceAmbiguity = true
                    break
                }
            } else {
                candidate.qualifiedName?.substringBeforeLast('.', "")?.let { candidatePackage ->
                    if (candidatePackage == "")
                        forceAmbiguity = true
                    else
                        conflictingPackages.add(candidatePackage)
                }
            }
        }
    }

    private fun findPotentialTypeAliasConflicts(target: PsiClass) {
        val candidates = findTypeAliasByShortName(targetShortName, target.project, target.project.allScope())
        for (candidate in candidates) {
            packagesWithTypeAliases.add(candidate.containingKtFile.packageFqName.asString())
        }
    }

    @TestOnly
    fun addConflict(fqName: String) {
        conflictingPackages.add(fqName.substringBeforeLast('.'))
    }

    /**
     * Checks if a reference with the short name of [targetClassFqName] in the given file will resolve
     * to the target class.
     *
     * @return true if it will definitely resolve to that class, false if it will definitely resolve to something else,
     * null if full resolve is required to answer that question.
     */
    fun canBeTargetReference(ref: KtSimpleNameExpression): ImpreciseResolveResult {
        attempts.incrementAndGet()
        // The names can be different if the target was imported via an import alias
        if (ref.getReferencedName() != targetShortName) {
            return UNSURE
        }

        // Names in expressions can conflict with local declarations and methods of implicit receivers,
        // so we can't find out what they refer to without a full resolve.
        val userType = ref.getStrictParentOfType<KtUserType>() ?: return UNSURE
        val parentAnnotation = userType.getParentOfTypeAndBranch<KtAnnotationEntry> { typeReference }
        if (forceAmbiguityForNonAnnotations && parentAnnotation == null) return UNSURE

        //For toplevel declarations it's fine to resolve by imports
        val declaration = parentAnnotation?.getParentOfType<KtDeclaration>(true)
        if (forceAmbiguityForInnerAnnotations && declaration?.parent !is KtFile) return UNSURE
        if (forceAmbiguity) return UNSURE

        val qualifiedCheckResult = checkQualifiedReferenceToTarget(ref)
        if (qualifiedCheckResult != null) return qualifiedCheckResult.returnValue

        val file = ref.containingKtFile
        var result: Result = Result.NothingFound
        when (file.packageFqName.asString()) {
            targetPackage -> result = result.changeTo(Result.Found)
            in conflictingPackages -> result = result.changeTo(Result.FoundOther)
            in packagesWithTypeAliases -> return UNSURE
        }

        for (importPath in file.getDefaultImports()) {
            result = analyzeSingleImport(result, importPath.fqName, importPath.isAllUnder, importPath.alias?.asString())
            if (result == Result.Ambiguity) return UNSURE
        }

        for (importDirective in file.importDirectives) {
            result = analyzeSingleImport(result, importDirective.importedFqName, importDirective.isAllUnder, importDirective.aliasName)
            if (result == Result.Ambiguity) return UNSURE
        }

        if (result.returnValue == MATCH) {
            trueHits.incrementAndGet()
        } else if (result.returnValue == NO_MATCH) {
            falseHits.incrementAndGet()
        }
        return result.returnValue
    }

    private fun analyzeSingleImport(result: Result, importedFqName: FqName?, isAllUnder: Boolean, aliasName: String?): Result {
        if (!isAllUnder) {
            if (importedFqName?.asString() == targetClassFqName &&
                (aliasName == null || aliasName == targetShortName)
            ) {
                return result.changeTo(Result.Found)
            } else if (importedFqName?.shortName()?.asString() == targetShortName &&
                importedFqName.parent().asString() in conflictingPackages &&
                aliasName == null
            ) {
                return result.changeTo(Result.FoundOther)
            } else if (importedFqName?.shortName()?.asString() == targetShortName &&
                importedFqName.parent().asString() in packagesWithTypeAliases &&
                aliasName == null
            ) {
                return Result.Ambiguity
            } else if (aliasName == targetShortName) {
                return result.changeTo(Result.FoundOther)
            }
        } else {
            when {
                importedFqName?.asString() == targetPackage -> return result.changeTo(Result.Found)
                importedFqName?.asString() in conflictingPackages -> return result.changeTo(Result.FoundOther)
                importedFqName?.asString() in packagesWithTypeAliases -> return Result.Ambiguity
            }
        }
        return result
    }

    private fun checkQualifiedReferenceToTarget(ref: KtSimpleNameExpression): Result? {
        // A qualified name can resolve to the target element even if it's not imported,
        // but it can also resolve to something else e.g. if the file defines a class with the same name
        // as the top-level package of the target class.
        val qualifier = (ref.parent as? KtUserType)?.qualifier
        if (qualifier != null) {
            if (qualifier.text == targetPackage) return Result.Ambiguity
            return Result.FoundOther
        }
        return null
    }

    enum class Result(val returnValue: ImpreciseResolveResult) {
        NothingFound(NO_MATCH),
        Found(MATCH),
        FoundOther(NO_MATCH),
        Ambiguity(UNSURE)
    }

    private fun Result.changeTo(newResult: Result): Result {
        if (this == Result.NothingFound || this.returnValue == newResult.returnValue) {
            return newResult
        }
        return Result.Ambiguity
    }
}