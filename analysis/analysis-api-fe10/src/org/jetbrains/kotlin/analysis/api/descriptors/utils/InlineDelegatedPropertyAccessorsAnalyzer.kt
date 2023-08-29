/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.descriptors.accessors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext

@OptIn(KtAnalysisNonPublicApi::class)
fun MutableSet<KtFile>.collectReachableInlineDelegatedPropertyAccessors() {
    if (isEmpty()) return

    // One of the compiler lowerings, namely `PropertyReferenceLowering`,
    // optimizes usages of property references in some cases,
    // and if a containing delegated property accessor is inline,
    // it might need this accessor's bytecode.
    //
    // If an accessor function is defined in a different module,
    // IDE tries to acquire its bytecode via the index, however,
    // the index doesn't cover classfiles from compiler output,
    // so the lowering fails.
    //
    // To solve the problem, we need to find all delegated properties with inline accessors
    // reachable from files that will be compiled, and include files these inline accessors
    // to the set of files that will be compiled (and do the same for these files recursively).
    // As it's basically a DAG traversal, we can keep a queue instead of making recursive calls.
    val allFiles = this
    val filesQueueToAnalyze = ArrayDeque(allFiles)
    val collector = InlineFunctionsCollector(allFiles.first().project, reifiedInlineFunctionsOnly = false) { declaration ->
        val containingFile = declaration.containingKtFile
        if (allFiles.add(containingFile)) {
            filesQueueToAnalyze += containingFile
        }
    }
    while (filesQueueToAnalyze.isNotEmpty()) {
        val file = filesQueueToAnalyze.removeFirst()
        analyze(file) {
            require(this is KtFe10AnalysisSession) {
                "K2 implementation shouldn't call this code"
            }
            file.accept(InlineDelegatedPropertyAccessorsAnalyzer(analysisContext, collector))
        }
    }
}

@OptIn(KtAnalysisNonPublicApi::class)
fun List<KtFile>.collectReachableInlineDelegatedPropertyAccessors(): List<KtFile> {
    if (isEmpty()) return this

    val allFiles = mutableSetOf<KtFile>()
    allFiles.addAll(this)
    allFiles.collectReachableInlineDelegatedPropertyAccessors()
    return allFiles.toList()
}

internal class InlineDelegatedPropertyAccessorsAnalyzer(
    private val analysisContext: Fe10AnalysisContext,
    private val collector: InlineFunctionsCollector
) : KtTreeVisitorVoid() {

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        val isDelegate = property.hasDelegateExpression()
        if (!isDelegate) return

        val bindingContext = analysisContext.analyze(property)
        val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property)
        if (descriptor !is VariableDescriptorWithAccessors) return

        descriptor.accessors.forEach { accessor ->
            collector.checkResolveCall(bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor))
        }
    }
}
