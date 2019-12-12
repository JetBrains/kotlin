package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.android.sdk.AndroidSdkData
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import org.jetbrains.kotlin.tools.projectWizard.core.service.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.plugins.gradle.action.ImportProjectFromScriptAction
import java.nio.file.Path

class IdeaFileSystemService : FileSystemService {
    override fun createDirectory(path: Path): TaskResult<Unit> = safe {
        runWriteAction<Unit> {
            VfsUtil.createDirectoryIfMissing(path.toString())
        }
    }

    override fun createFile(path: Path, text: String): TaskResult<Unit> = safe {
        runWriteAction {
            val directoryPath = path.parent
            val directory = VfsUtil.createDirectoryIfMissing(directoryPath.toFile().toString())!!
            val virtualFile = directory.createChildData(this, path.fileName.toString())
            VfsUtil.saveText(virtualFile, text)
        }
    }
}

class IdeaGradleService(private val project: Project) : GradleService {

    // We have to call action directly as there is no common way
    // to import Gradle project in all IDEAs from 183 to 193
    override fun importProject(
        path: Path,
        modulesIrs: List<ModuleIR>
    ): TaskResult<Unit> = safe {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path.toString())!!
        val dataContext = SimpleDataContext.getSimpleContext(
            mapOf(
                CommonDataKeys.PROJECT.name to project,
                CommonDataKeys.VIRTUAL_FILE.name to virtualFile
            ),
            null
        )
        val action = ImportProjectFromScriptAction()
        val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, dataContext)
        action.actionPerformed(event)
    }
}

class IdeaMavenService(private val project: Project) : MavenService {
    override fun importProject(
        path: Path,
        modulesIrs: List<ModuleIR>
    ): TaskResult<Unit> = safe {
        val mavenProjectManager = MavenProjectsManager.getInstance(project)

        val rootFile = LocalFileSystem.getInstance().findFileByPath(path.toString())!!
        mavenProjectManager.addManagedFilesOrUnignore(rootFile.findAllPomFiles())
    }

    private fun VirtualFile.findAllPomFiles(): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()

        fun VirtualFile.find() {
            when {
                !isDirectory && name == "pom.xml" -> result += this
                isDirectory -> children.forEach(VirtualFile::find)
            }
        }

        find()
        return result
    }
}

class IdeaAndroidService : AndroidService {
    override fun isValidAndroidSdk(path: Path): Boolean =
        //todo use android plugin for that?
        AndroidServiceImpl().isValidAndroidSdk(path)
}

