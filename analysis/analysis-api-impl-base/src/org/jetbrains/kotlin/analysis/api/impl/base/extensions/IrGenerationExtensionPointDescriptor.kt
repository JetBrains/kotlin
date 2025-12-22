/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.extensions

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

object IrGenerationExtensionPointDescriptor : ProjectExtensionDescriptor<IrGenerationExtension>(
    name = IrGenerationExtension.name,
    extensionClass = IrGenerationExtension.extensionClass,
)
