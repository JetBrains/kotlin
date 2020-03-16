/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponentFactory

class LegacyKotlinJsComponentPlugin
@javax.inject.Inject
constructor(
    private val softwareComponentFactory: SoftwareComponentFactory
) : Plugin<Project> {
    override fun apply(target: Project) {
//      create an adhoc component
        val adhocComponent = softwareComponentFactory.adhoc("myAdhocComponent")
        // add it to the list of components that this project declares
        target.components.add(adhocComponent)
    }
}