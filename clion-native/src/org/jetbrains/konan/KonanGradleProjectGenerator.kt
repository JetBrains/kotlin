package org.jetbrains.konan

import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WebProjectTemplate
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.ColorUtil
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.layout.*
import com.intellij.ui.speedSearch.ListWithFilter
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.HtmlPanel
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.plugins.gradle.service.project.GradleProjectOpenProcessor
import javax.swing.*
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED


class KonanGradleProjectGenerator : WebProjectTemplate<Any>() {
    override fun getDescription(): String? {
        return null
    }

    override fun getLogo(): Icon? {
        return KotlinIcons.NATIVE
    }

    private var template: KonanProjectTemplate? = null
    private var loadedTemplate: KonanProjectTemplate.Loaded? = null

    override fun getName(): String = "Kotlin/Native Application"

    override fun generateProject(project: Project, baseDir: VirtualFile, settings: Any, module: Module) {
        loadedTemplate = template?.load()

        val filesToOpen = mutableListOf<VirtualFile>()
        runWriteAction {
            for (file in loadedTemplate?.templateFiles.orEmpty()) {
                val vFile = file.create(baseDir)
                if (file.openInEditor) filesToOpen += vFile
            }
        }

        GradleProjectOpenProcessor.attachGradleProjectAndRefresh(project, baseDir.path)

        filesToOpen.forEach { vf ->
            PsiNavigationSupport.getInstance().createNavigatable(project, vf, -1).navigate(true)
        }
    }

    override fun createPeer(): ProjectGeneratorPeer<Any> {
        return object: GeneratorPeerImpl<Any>(Unit, JPanel()) {
            override fun buildUI(settingsStep: SettingsStep) {
                settingsStep.addSettingsComponent(getSettingsPanel())
            }
        }
    }

    private fun getSettingsPanel(): JComponent {
        val templateList = TemplateListPanel()
        val readme = ReadmePanel()
        val modalityState = ApplicationManager.getApplication().currentModalityState
        ApplicationManager.getApplication().executeOnPooledThread {
            val templates = KonanProjectTemplate.listAll()
            ApplicationManager.getApplication().invokeLater({
                templateList.setTemplates(templates)
                templateList.onTemplateSelected { selectedTemplate ->
                    template = selectedTemplate
                    readme.setHtml(template?.htmlDescription)
                }
                templateList.selectDefaultTemplate()
            }, modalityState)
        }

        return panel {
            row("Project template:") {}
            row { templateList.component(CCFlags.growX) }
            row { readme.component(CCFlags.growX, CCFlags.growY) }
        }/*.apply {
            // We are already inside a scroll pane, so we need set size to make sure outer panel
            // is not overflown.
            // class cast exception
            ((layout as MigLayout).constraintMap[readme.component] as CC).maxHeight("240px")
        }*/
    }

}

// Ideally, we'd want to add a scrollbar to the readme panel itself,
// but unfortunately it conflicts with the outer scroll pane on the
// whole project generator.
private class ReadmePanel {
    private val readme = htmlPanel("")

    fun setHtml(html: String?) {
        readme.text = html ?: html("No description.")
        readme.caretPosition = 0
        component.parent?.revalidate()
    }

    val component: JComponent = ScrollPaneFactory.createScrollPane(
            readme, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER
    ).apply { border = null }
}

private class TemplateListPanel {
    private lateinit var templates: List<KonanProjectTemplate>
    private val templateListModel = DefaultListModel<String>()
    private val templateList = JBList<String>(templateListModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        visibleRowCount = 5
    }

    private val listWithFilter = ListWithFilter.wrap(
            templateList,
            ScrollPaneFactory.createScrollPane(templateList), { it }
    )

    fun setTemplates(templates: List<KonanProjectTemplate>) {
        this.templates = templates
        templates.forEach { templateListModel.addElement(it.visibleName) }
        listWithFilter.revalidate()
    }

    fun onTemplateSelected(callback: (KonanProjectTemplate?) -> Unit) {
        templateList.addListSelectionListener {
            callback(templates.getOrNull(templateList.selectedIndex))
        }
    }

    fun selectDefaultTemplate() {
        if (templates.isNotEmpty()) templateList.selectedIndex = 0
    }

    val component: JComponent get() = listWithFilter
}

private fun htmlPanel(body: String): HtmlPanel {
    val textArea = object : HtmlPanel() {
        override fun getBody(): String {
            return body
        }
    }
    textArea.setBody(body)
    textArea.background = UIUtil.getPanelBackground()
    textArea.isEditable = false
    return textArea
}

private fun html(body: String): String = """
        <html>
        <head>
            ${UIUtil.getCssFontDeclaration(UIUtil.getLabelFont())}
            <style>body {background: #${ColorUtil.toHex(UIUtil.getPanelBackground())};}</style>
        </head>
        <body>
            $body
        </body>
        </html>
    """
