/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven

import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.idea.maven.model.MavenArtifact
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager

//BUNCH: 183
fun scheduleArtifactsDownloading(
    projectsManager: MavenProjectsManager,
    projects: List<MavenProject>,
    toBeDownloaded: List<MavenArtifact>
) {
    //true, false, AsyncResult()
    projectsManager.scheduleArtifactsDownloading(projects, toBeDownloaded, true, false, AsyncPromise())
}