package com.jetbrains.mobile.execution

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.WrappingRunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import com.jetbrains.mobile.MobileBundle
import javax.swing.Icon

class MobileBeforeRunTaskProvider : BeforeRunTaskProvider<MobileBeforeRunTaskProvider.Task>() {
    override fun getName(): String = MobileBundle.message("build")
    override fun getId(): Key<Task> = ID
    override fun getIcon(): Icon = AllIcons.Actions.Compile
    override fun isSingleton(): Boolean = true

    override fun canExecuteTask(configuration: RunConfiguration, task: Task): Boolean =
        configuration is MobileRunConfigurationBase

    override fun createTask(configuration: RunConfiguration): Task? =
        if (configuration !is MobileRunConfigurationBase) null
        else Task()

    override fun executeTask(
        context: DataContext,
        runConfiguration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: Task
    ): Boolean {
        val configuration = (runConfiguration as? WrappingRunConfiguration<*>)?.peer
            ?: runConfiguration

        if (configuration !is MobileRunConfigurationBase) return false
        val device = environment.executionTarget as? Device ?: return false

        return MobileBuild.build(configuration, device)
    }

    class Task : BeforeRunTask<Task>(ID) {
        init {
            isEnabled = true
        }
    }

    companion object {
        private val ID = Key<Task>("MobileBuild")
        private val log = logger<MobileBeforeRunTaskProvider>()
    }
}