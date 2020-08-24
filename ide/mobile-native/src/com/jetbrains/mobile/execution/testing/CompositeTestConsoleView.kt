package com.jetbrains.mobile.execution.testing

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.*
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import javax.swing.tree.TreeSelectionModel

class CompositeTestConsoleView(consoleProperties: SMTRunnerConsoleProperties, consoles: List<SMTRunnerConsoleView>) :
    SMTRunnerConsoleView(consoleProperties, SMTestRunnerConnectionUtil.getSplitterPropertyName(consoleProperties.testFrameworkName)) {
    init {
        assert(consoles.isNotEmpty())

        initUI()
        resultsViewer.treeView!!.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION

        addAttachToProcessListener { handler ->
            val project = consoleProperties.project
            val testFrameworkName = consoleProperties.testFrameworkName

            val outputConsumer = OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties)
            outputConsumer.setTestingStartedHandler {
                val eventsProcessor = GeneralToSMTRunnerEventsConvertor(project, resultsViewer.testsRootNode, testFrameworkName)
                eventsProcessor.addEventsListener(resultsViewer)
                outputConsumer.setProcessor(eventsProcessor)
            }

            val compositeRoot = resultsViewer.root as SMTestProxy.SMRootTestProxy
            consoles.forEach {
                val root = it.resultsViewer.root as SMTestProxy.SMRootTestProxy
                compositeRoot.addChild(root)
            }

            // Redirect output and testing events from all the consoles to the current one:
            project.messageBus.connect(this)
                .subscribe(SMTRunnerEventsListener.TEST_STATUS, resultsViewer)

            handler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    outputConsumer.finishTesting()
                }
            })

            outputConsumer.startTesting()
        }
    }
}
