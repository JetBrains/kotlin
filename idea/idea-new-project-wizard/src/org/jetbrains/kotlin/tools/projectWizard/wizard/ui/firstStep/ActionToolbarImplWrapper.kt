/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl

// needed for <=192 to work correctly
abstract class ActionToolbarImplWrapper(
    place: String,
    actionGroup: ActionGroup,
    horizontal: Boolean
) : ActionToolbarImpl(place, actionGroup, horizontal)