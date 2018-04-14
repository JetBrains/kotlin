/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.resolve.BindingContext

class OldAnalyzer {
    private val issues = mutableListOf<Issue>()

    fun functionIssue(init: FunctionDeclarationIssue.() -> Unit) {
        val issue = FunctionDeclarationIssue()
        issue.init()
        issues.add(issue)
    }

    fun execute(
        irModule: IrModuleFragment,
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext
    ) {
        issues.forEach { it.execute(irModule, moduleDescriptor, bindingContext) }
    }
}

fun oldAnalyzer(
    init: OldAnalyzer.() -> Unit
): OldAnalyzer {
    val analyzer = OldAnalyzer()
    analyzer.init()
    return analyzer
}