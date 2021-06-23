/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.gradle.enterprise.gradleplugin.testdistribution.TestDistributionExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.os.OperatingSystem


fun Test.configureTestDistribution(configure: TestDistributionExtension.() -> Unit = {}) {
    val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild
    val testDistributionEnabled =
        project.findProperty("kotlin.build.test.distribution.enabled")?.toString()?.toBoolean() ?: false

    useJUnitPlatform()
    extensions.configure(TestDistributionExtension::class.java) {
        enabled.set(testDistributionEnabled)
        maxRemoteExecutors.set(20)
        if (isTeamcityBuild) {
            requirements.set(setOf("os=${OperatingSystem.current().familyName}"))
        } else {
            maxLocalExecutors.set(0)
        }
        configure()
    }
}

fun Test.isTestDistributionEnabled(): Boolean =
    extensions.findByType(TestDistributionExtension::class.java)?.enabled?.orNull ?: false
