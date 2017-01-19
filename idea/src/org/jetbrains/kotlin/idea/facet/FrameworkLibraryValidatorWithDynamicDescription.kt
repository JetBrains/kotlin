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

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.impl.ui.libraries.LibrariesValidatorContext
import com.intellij.facet.ui.FacetConfigurationQuickFix
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.facet.ui.ValidationResult
import com.intellij.facet.ui.libraries.FrameworkLibraryValidator
import com.intellij.ide.IdeBundle
import com.intellij.openapi.roots.ui.configuration.libraries.AddCustomLibraryDialog
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.framework.CommonStandardLibraryDescription
import org.jetbrains.kotlin.idea.framework.JSLibraryStdDescription
import org.jetbrains.kotlin.idea.framework.JavaRuntimeLibraryDescription
import javax.swing.JComponent

// Based on com.intellij.facet.impl.ui.libraries.FrameworkLibraryValidatorImpl
class FrameworkLibraryValidatorWithDynamicDescription(
        private val context: LibrariesValidatorContext,
        private val validatorsManager: FacetValidatorsManager,
        private val libraryCategoryName: String,
        private val getTargetPlatform: () -> TargetPlatformKind<*>
) : FrameworkLibraryValidator() {
    private val TargetPlatformKind<*>.libraryDescription: CustomLibraryDescription
        get() {
            val project = context.module.project
            return when (this) {
                is TargetPlatformKind.Jvm -> JavaRuntimeLibraryDescription(project)
                is TargetPlatformKind.JavaScript -> JSLibraryStdDescription(project)
                is TargetPlatformKind.Common -> CommonStandardLibraryDescription(project)
            }
        }

    private fun checkLibraryIsConfigured(targetPlatform: TargetPlatformKind<*>): Boolean {
        // TODO: propose to configure kotlin-stdlib-common once it's available
        if (targetPlatform == TargetPlatformKind.Common) return true

        if (KotlinVersionInfoProvider.EP_NAME.extensions.any { it.getLibraryVersions(context.module, targetPlatform).isNotEmpty() }) return true

        val libraryDescription = targetPlatform.libraryDescription
        val libraryKinds = libraryDescription.suitableLibraryKinds
        var found = false
        val presentationManager = LibraryPresentationManager.getInstance()
        context.rootModel
                .orderEntries()
                .using(context.modulesProvider)
                .recursively()
                .librariesOnly()
                .forEachLibrary { library ->
                    if (presentationManager.isLibraryOfKind(library, context.librariesContainer, libraryKinds)) {
                        found = true
                    }
                    !found
                }
        return found
    }

    override fun check(): ValidationResult {
        val targetPlatform = getTargetPlatform()

        if (checkLibraryIsConfigured(targetPlatform)) {
            val conflictingPlatforms = TargetPlatformKind.ALL_PLATFORMS.filter {
                it != TargetPlatformKind.Common && it.name != targetPlatform.name && checkLibraryIsConfigured(it)
            }
            if (conflictingPlatforms.isNotEmpty()) {
                val platformText = conflictingPlatforms.mapTo(LinkedHashSet()) { it.name }.joinToString()
                return ValidationResult("Libraries for the following platform are also present in the module dependencies: $platformText")
            }

            return ValidationResult.OK
        }


        return ValidationResult(
                IdeBundle.message("label.missed.libraries.text", libraryCategoryName),
                LibrariesQuickFix(targetPlatform.libraryDescription)
        )
    }

    private inner class LibrariesQuickFix(
            private val myDescription: CustomLibraryDescription
    ) : FacetConfigurationQuickFix(IdeBundle.message("button.fix")) {
        override fun run(place: JComponent) {
            val dialog = AddCustomLibraryDialog.createDialog(myDescription, context.librariesContainer,
                                                             context.module, context.modifiableRootModel,
                                                             null)
            dialog.show()
            validatorsManager.validate()
        }
    }
}
