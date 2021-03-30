gradle.taskGraph.whenReady { taskGraph ->
    taskGraph.allTasks.each { task ->
        def taskSuperClass = task.class
        while (taskSuperClass != Object.class) {
            if (taskSuperClass.canonicalName == "org.jetbrains.kotlin.gradle.tasks.KotlinTest") {
                try {
                    KotlinMppTestLogger.logTestReportLocation(task.reports?.html?.entryPoint?.path)
                    KotlinMppTestLogger.configureTestEventLogging(task)
                    task.testLogging.showStandardStreams = false
                }
                catch (all) {
                    logger.error("", all)
                }
                return
            } else {
                taskSuperClass = taskSuperClass.superclass
            }
        }
    }
}