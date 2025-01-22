/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.nordstrom.automation.junit.MethodWatcher
import org.junit.AssumptionViolatedException
import org.junit.internal.runners.model.ReflectiveCallable
import org.junit.runners.model.FrameworkMethod

class MuteWithDatabaseWatcher : MethodWatcher<FrameworkMethod> {
    override fun beforeInvocation(
        runner: Any?,
        child: FrameworkMethod?,
        callable: ReflectiveCallable?,
    ) {
//        println("NO EXECUTION SUPPRESSION")
        //throw AssumptionViolatedException("Failure without execution")
    }

    override fun afterInvocation(
        runner: Any?,
        child: FrameworkMethod?,
        callable: ReflectiveCallable?,
        thrown: Throwable?,
    ) {
        //if (thrown == null) return

        throw Exception("JUnit4 Expected failure")
        //throw AssumptionViolatedException("Failure suppressed: ${thrown?.message}")
    }

    override fun supportedType(): Class<FrameworkMethod> = FrameworkMethod::class.java
}