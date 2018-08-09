package org.jetbrains.kotlin.idea.configuration.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.migration.CodeMigrationToggleAction
import javax.swing.JComponent

internal class MigrationNotificationDialog(val project: Project, migrationInfo: MigrationInfo) : DialogWrapper(project, false) {
    private val form = MigrationNotificationForm()

    init {
        title = "Kotlin Migration (Experimental)"
        setOKButtonText("Migrate")

        form.libraryInfo.isVisible = migrationInfo.oldStdlibVersion != migrationInfo.newStdlibVersion
        form.libraryInfo.text = "Standard library: ${migrationInfo.oldStdlibVersion} to ${migrationInfo.newStdlibVersion}"

        form.languageVersionInfo.isVisible = migrationInfo.oldLanguageVersion != migrationInfo.newLanguageVersion
        form.languageVersionInfo.text = "Language version: ${migrationInfo.oldLanguageVersion} to ${migrationInfo.newLanguageVersion}"

        form.apiVersionInfo.isVisible = migrationInfo.oldApiVersion != migrationInfo.newApiVersion
        form.apiVersionInfo.text = "API version: ${migrationInfo.oldApiVersion} to ${migrationInfo.newApiVersion}"

        form.disableMigrationDetectionCheckBox.isSelected = !CodeMigrationToggleAction.isEnabled(project)

        init()
    }

    override fun createCenterPanel(): JComponent = form.mainPanel

    private fun saveSettings() {
        CodeMigrationToggleAction.setEnabled(project, !form.disableMigrationDetectionCheckBox.isSelected)
    }

    override fun doCancelAction() {
        saveSettings()
        super.doCancelAction()
    }

    override fun doOKAction() {
        saveSettings()
        super.doOKAction()
    }

}