/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runners.BlockJUnit4ClassRunner

class JUnit3WithIdeaConfigurationRunner(klass: Class<*>?) : JUnit38ClassRunner(klass) {

    companion object {
        init {
            IdeaSystemPropertiesForParallelRunConfigurator.setProperties()
        }
    }
}

class JUnit4WithIdeaConfigurationRunner(klass: Class<*>?) : BlockJUnit4ClassRunner(klass) {

    companion object {
        init {
            IdeaSystemPropertiesForParallelRunConfigurator.setProperties()
        }
    }
}