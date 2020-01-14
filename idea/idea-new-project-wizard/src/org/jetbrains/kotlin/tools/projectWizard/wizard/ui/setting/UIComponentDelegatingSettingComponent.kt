/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingType
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.UIComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.valueForSetting
import javax.swing.JComponent

abstract class UIComponentDelegatingSettingComponent<V : Any, T : SettingType<V>>(
    reference: SettingReference<V, T>,
    valuesReadingContext: ValuesReadingContext
) : SettingComponent<V, T>(reference, valuesReadingContext) {
    abstract val uiComponent: UIComponent<V>

    // As there is one in UIComponent
    override val validationIndicator: ValidationIndicator? = null

    override fun onInit() {
        super.onInit()
        if (value == null) {
            if (value == null) {
                value = uiComponent.valueForSetting(setting)
            }
        }
        value?.let(uiComponent::updateUiValue)
    }

    override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) { uiComponent.component }
}