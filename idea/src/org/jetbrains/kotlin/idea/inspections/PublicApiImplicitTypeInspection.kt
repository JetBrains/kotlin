/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.effectiveVisibility
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class PublicApiImplicitTypeInspection : AbstractImplicitTypeInspection(
    { element, _ ->
        element.containingClassOrObject?.isLocal != true &&
                when (element) {
                    is KtFunction -> !element.isLocal
                    is KtProperty -> !element.isLocal
                    else -> false
                } && run {
            val callableMemberDescriptor = element.resolveToDescriptorIfAny() as? CallableMemberDescriptor
            callableMemberDescriptor?.effectiveVisibility()?.toVisibility()?.isPublicAPI == true
        }
    }
) {
    override val problemText = "For API stability, it's recommended to specify explicitly public & protected declaration types"
}