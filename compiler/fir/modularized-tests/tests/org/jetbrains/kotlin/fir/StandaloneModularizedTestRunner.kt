/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import kotlin.system.exitProcess

object StandaloneModularizedTestRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        val runner = JUnit38ClassRunner(FirResolveModularizedTotalKotlinTest::class.java)
        var ok = true
        runner.run(
            RunNotifier().apply {
                addListener(
                    object : RunListener() {
                        override fun testAssumptionFailure(failure: Failure?) {
                            ok = false
                            println("assertion failure: $failure")
                            System.out.flush()
                            failure?.exception?.printStackTrace()
                            System.err.flush()
                        }

                        override fun testFailure(failure: Failure?) {
                            ok = false
                            println("test failure: $failure")
                            System.out.flush()
                            failure?.exception?.printStackTrace()
                            System.err.flush()
                        }

                        override fun testIgnored(description: Description?) {
                            println("test ignored: $description")
                        }

                        override fun testStarted(description: Description?) {
                            println("test started: $description")
                        }

                        override fun testFinished(description: Description?) {
                            println("test finished: $description")
                        }
                    },
                )
            },
        )

        println("runner exit")
        exitProcess(if (ok) 0 else 1)

    }
}