package org.jetbrains.kotlin.jklib.test.runners

import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class JKlibJavaSourceConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val registeredDirectives = module.directives
        val jdkKind = JvmEnvironmentConfigurator.extractJdkKind(registeredDirectives)
        JvmEnvironmentConfigurator.getJdkHome(jdkKind)?.let { configuration.put(JVMConfigurationKeys.JDK_HOME, it) }

        when (jdkKind) {
            TestJdkKind.MOCK_JDK, TestJdkKind.MODIFIED_MOCK_JDK -> {
                configuration.put(JVMConfigurationKeys.NO_JDK, true)
                var home = File(KtTestUtil.getHomeDirectory())
                if (home.name == "tests-against-klib") {
                     home = home.parentFile.parentFile
                }
                val mockJdkPath = File(home, "compiler/testData/mockJDK/jre/lib/rt.jar")
                
                try {
                    val tempJdk = File.createTempFile("mock-rt", ".jar")
                    tempJdk.deleteOnExit()
                    java.security.AccessController.doPrivileged(java.security.PrivilegedAction {
                        mockJdkPath.copyTo(tempJdk, overwrite = true)
                    })
                    configuration.addJvmClasspathRoot(tempJdk)
                } catch (e: Throwable) {
                    // Fallback or ignore. The whitelist should prevent AccessControlException, 
                    // and doPrivileged ensures we can copy it if needed.
                }
            }
            else -> {
                JvmEnvironmentConfigurator.getJdkClasspathRoot(jdkKind)?.let { configuration.addJvmClasspathRoot(it) }
            }
        }
        configuration.configureJdkClasspathRoots()

        val javaFiles = module.files.filter { it.name.endsWith(".java") }
        if (javaFiles.isEmpty()) return
        javaFiles.forEach { testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(it) }
        configuration.addJavaSourceRoot(testServices.sourceFileProvider.getJavaSourceDirectoryForModule(module))
    }
}
