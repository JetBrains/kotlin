// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// WITH_STDLIB
import java.awt.EventQueue
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import javax.swing.SwingUtilities

fun viaThread() {
    var unstable = ""
    Thread {
        println(<!CV_DIAGNOSTIC!>unstable<!>)
    }
    unstable = "hello"
}

fun viaExecutor(executor: Executor) {
    var unstable = ""
    executor.execute {
        println(<!CV_DIAGNOSTIC!>unstable<!>)
    }
    unstable = "hello"
}

fun viaExecutorService(executor: ExecutorService) {
    var unstable = ""
    executor.submit {
        println(<!CV_DIAGNOSTIC!>unstable<!>)
    }
    unstable = "hello"
}

fun viaSwingUtilities() {
    var unstable = ""
    SwingUtilities.invokeLater {
        println(<!CV_DIAGNOSTIC!>unstable<!>)
    }
    unstable = "hello"
}

fun viaEventQueue() {
    var unstable = ""
    EventQueue.invokeLater {
        println(<!CV_DIAGNOSTIC!>unstable<!>)
    }
    unstable = "hello"
}

fun viaNonAllowlistedIsNotReported(barRegular: (() -> Unit) -> Unit) {
    var unstable = ""
    barRegular {
        println(unstable)
    }
    unstable = "hello"
}

/* GENERATED_FIR_TAGS: assignment, flexibleType, functionDeclaration, functionalType, javaFunction, lambdaLiteral,
localProperty, propertyDeclaration, samConversion, starProjection, stringLiteral */
