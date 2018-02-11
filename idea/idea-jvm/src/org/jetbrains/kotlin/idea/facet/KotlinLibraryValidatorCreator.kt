/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.impl.ui.libraries.DelegatingLibrariesValidatorContext
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorValidator
import com.intellij.facet.ui.FacetValidatorsManager
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.facet.KotlinFacetEditorGeneralTab.EditorComponent

class KotlinLibraryValidatorCreator : KotlinFacetValidatorCreator() {
    override fun create(editor: EditorComponent, validatorsManager: FacetValidatorsManager, editorContext: FacetEditorContext): FacetEditorValidator {
        return FrameworkLibraryValidatorWithDynamicDescription(
                DelegatingLibrariesValidatorContext(editorContext),
                validatorsManager,
                "kotlin"
        ) { editor.targetPlatformComboBox.selectedItem as TargetPlatformKind<*> }
    }
}