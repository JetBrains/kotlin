/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.util.Consumer
import org.jdom.Element
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.utils.PathUtil
import javax.swing.JComponent

class KotlinSdkType : SdkType("KotlinSDK") {
    companion object {
        @JvmField val INSTANCE = KotlinSdkType()

        val defaultHomePath: String
            get() = PathUtil.kotlinPathsForIdeaPlugin.homePath.absolutePath

        @JvmOverloads
        fun setUpIfNeeded(checkIfNeeded: () -> Boolean = { true }) {
            with(ProjectSdksModel()) {
                reset(null)
                if (sdks.any { it.sdkType is KotlinSdkType }) return
                if (!checkIfNeeded()) return //do not create Kotlin SDK
                addSdk(INSTANCE, defaultHomePath, null)
                ApplicationManager.getApplication().invokeAndWait {
                    runWriteAction { apply(null, true) }
                }
            }
        }
    }

    override fun getPresentableName() = "Kotlin SDK"

    override fun getIcon() = KotlinIcons.SMALL_LOGO

    override fun isValidSdkHome(path: String?) = true

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String?) = "Kotlin SDK"

    override fun suggestHomePath() = null

    override fun sdkHasValidPath(sdk: Sdk) = true

    override fun getVersionString(sdk: Sdk) = bundledRuntimeVersion()

    override fun supportsCustomCreateUI() = true

    override fun showCustomCreateUI(sdkModel: SdkModel, parentComponent: JComponent, selectedSdk: Sdk?, sdkCreatedCallback: Consumer<Sdk>) {
        sdkCreatedCallback.consume(createSdkWithUniqueName(sdkModel.sdks.toList()))
    }

    fun createSdkWithUniqueName(existingSdks: Collection<Sdk>): ProjectJdkImpl {
        val sdkName = suggestSdkName(SdkConfigurationUtil.createUniqueSdkName(this, "", existingSdks), "")
        return ProjectJdkImpl(sdkName, this).apply {
            homePath = defaultHomePath
        }
    }

    override fun createAdditionalDataConfigurable(sdkModel: SdkModel, sdkModificator: SdkModificator) = null

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {

    }
}