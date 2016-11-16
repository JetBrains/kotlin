/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android.inspection

import com.android.resources.ResourceFolderType
import com.android.resources.ResourceType
import com.intellij.codeInsight.daemon.QuickFixActionRegistrar
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider
import com.intellij.openapi.module.ModuleUtil
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.inspections.CreateFileResourceQuickFix
import org.jetbrains.android.inspections.CreateValueResourceQuickFix
import org.jetbrains.android.util.AndroidResourceUtil
import org.jetbrains.kotlin.android.getReferredResourceOrManifestField
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference


class KotlinAndroidResourceQuickFixProvider : UnresolvedReferenceQuickFixProvider<KtSimpleNameReference>() {

    override fun registerFixes(ref: KtSimpleNameReference, registrar: QuickFixActionRegistrar) {
        val expression = ref.expression
        val contextModule = ModuleUtil.findModuleForPsiElement(expression) ?: return
        val facet = AndroidFacet.getInstance(contextModule) ?: return
        val manifest = facet.manifest ?: return
        manifest.`package`.value ?: return
        val contextFile = expression.containingFile ?: return

        val info = getReferredResourceOrManifestField(facet, expression, true)
        if (info == null || info.isFromManifest) {
            return
        }

        val resourceType = ResourceType.getEnum(info.className)

        if (AndroidResourceUtil.ALL_VALUE_RESOURCE_TYPES.contains(resourceType)) {
            registrar.register(CreateValueResourceQuickFix(facet, resourceType, info.fieldName, contextFile, true))
        }

        val folderType = AndroidResourceUtil.XML_FILE_RESOURCE_TYPES[resourceType] as? ResourceFolderType
        if (folderType != null) {
            registrar.register(CreateFileResourceQuickFix(facet, folderType, info.fieldName, contextFile, true))
        }
    }

    override fun getReferenceClass() = KtSimpleNameReference::class.java
}

