/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenImportListener
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.kotlin.idea.configuration.notifyOutdatedBundledCompilerIfNecessary

class MavenImportListener(val project: Project) {
    init {
        project.messageBus.connect().subscribe(
            MavenImportListener.TOPIC,
            MavenImportListener { _: Collection<MavenProject>, _: List<Module> ->
                notifyOutdatedBundledCompilerIfNecessary(project)
            }
        )
    }
}