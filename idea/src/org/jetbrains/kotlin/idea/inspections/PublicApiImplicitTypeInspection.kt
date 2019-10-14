/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.resolve.checkers.ExplicitApiDeclarationChecker
import javax.swing.JComponent

class PublicApiImplicitTypeInspection(
    @JvmField var reportInternal: Boolean = false,
    @JvmField var reportPrivate: Boolean = false
) : AbstractImplicitTypeInspection(
    { element, inspection ->
        val shouldCheckForPublic = element.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode) == ExplicitApiMode.DISABLED
        val callableMemberDescriptor = element.resolveToDescriptorIfAny() as? CallableMemberDescriptor
        val forInternal = (inspection as PublicApiImplicitTypeInspection).reportInternal
        val forPrivate = inspection.reportPrivate
        ExplicitApiDeclarationChecker.returnTypeRequired(element, callableMemberDescriptor, shouldCheckForPublic, forInternal, forPrivate)
    }
) {

    override val problemText: String
        get() {
            return if (!reportInternal && !reportPrivate)
                "For API stability, it's recommended to specify explicitly public & protected declaration types"
            else
                "For API stability, it's recommended to specify explicitly declaration types"
        }

    override fun createOptionsPanel(): JComponent? {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox("Apply also to internal members", "reportInternal")
        panel.addCheckbox("Apply also to private members", "reportPrivate")
        return panel
    }
}