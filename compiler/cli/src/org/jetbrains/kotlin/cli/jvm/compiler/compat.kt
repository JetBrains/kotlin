/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

fun setupIdeaStandaloneExecution() {
    System.getProperties().setProperty("idea.home.path",  System.getProperty("java.io.tmpdir"))
    System.getProperties().setProperty("project.structure.add.tools.jar.to.new.jdk", "false")
    System.getProperties().setProperty("psi.track.invalidation", "true")
    System.getProperties().setProperty("psi.incremental.reparse.depth.limit", "1000")
    System.getProperties().setProperty("ide.hide.excluded.files", "false")
}