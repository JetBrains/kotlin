/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.gradle.enterprise.gradleplugin.testdistribution.TestDistributionExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.os.OperatingSystem


fun Test.configureTestDistribution(configure: TestDistributionExtension.() -> Unit = {}) {
    if(extensions.findByType(TestDistributionExtension::class.java) == null) return

    val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild

    useJUnitPlatform()
    extensions.configure(TestDistributionExtension::class.java) {
        enabled.set(true)
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
