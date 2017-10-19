package com.intellij.debugger.streams.kotlin.exec

import com.intellij.debugger.impl.OutputChecker
import com.intellij.debugger.streams.kotlin.LibraryManager
import com.intellij.debugger.streams.lib.LibrarySupportProvider
import com.intellij.debugger.streams.test.TraceExecutionTestCase
import com.intellij.execution.configurations.JavaParameters
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.asJava.classes.FakeLightClassForFileOfPackage
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import java.io.File
import java.nio.file.Paths

/**
 * @author Vitaliy.Bibaev
 */
abstract class KotlinEvaluationTestCase : TraceExecutionTestCase() {
  private companion object {
    val STDLIB_JAR_NAME = "kotlin-stdlib.jar"
  }

  abstract val appName: String
  abstract val librarySupport: LibrarySupportProvider

  override final fun getLibrarySupportProvider(): LibrarySupportProvider = librarySupport

  override fun setUpModule() {
    super.setUpModule()
    ApplicationManager.getApplication().runWriteAction {
      VfsRootAccess.allowRootAccess(LibraryManager.LIBRARIES_DIRECTORY)
      PsiTestUtil.addLibrary(myModule, "${LibraryManager.LIBRARIES_DIRECTORY}/$STDLIB_JAR_NAME")
    }
  }

  override fun createJavaParameters(mainClass: String?): JavaParameters {
    val javaParameters = super.createJavaParameters(mainClass)
    javaParameters.classPath.add("${LibraryManager.LIBRARIES_DIRECTORY}/$STDLIB_JAR_NAME")
    return javaParameters
  }

  override fun initOutputChecker(): OutputChecker {
    return KotlinOutputChecker(testAppPath, appOutputPath)
  }

  override fun createLocalProcess(className: String?) {
    super.createLocalProcess(className + "Kt")
  }

  override fun createBreakpoints(className: String) {
    val psiClasses = ApplicationManager.getApplication().runReadAction(Computable<Array<PsiClass>> {
      JavaPsiFacade.getInstance(myProject)
          .findClasses(className, GlobalSearchScope.allScope(myProject))
    })

    for (psiClass in psiClasses) {
      if (psiClass is KtLightClassForFacade) {
        val files = psiClass.files
        for (jetFile in files) {
          createBreakpoints(jetFile)
        }
      } else if (psiClass is FakeLightClassForFileOfPackage) {
        // skip, because we already create breakpoints using KotlinLightClassForPackage
      } else {
        createBreakpoints(psiClass.containingFile)
      }
    }
  }

  override fun getTestAppPath(): String {
    return Paths.get(File("").absolutePath, "/testData/exec/$appName").toString()
  }
}
