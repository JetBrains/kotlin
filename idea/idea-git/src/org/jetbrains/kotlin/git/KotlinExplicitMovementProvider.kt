/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Couple
import com.intellij.openapi.vcs.FilePath
import git4idea.checkin.GitCheckinExplicitMovementProvider
import org.jetbrains.kotlin.idea.actions.pathBeforeJ2K
import java.util.*

class KotlinExplicitMovementProvider : GitCheckinExplicitMovementProvider() {
    override fun isEnabled(project: Project): Boolean {
        return true
    }

    override fun getDescription(): String {
        return "Extra commit for .java > .kt renames"
    }

    override fun getCommitMessage(oldCommitMessage: String): String {
        return "Rename .java to .kt"
    }

    override fun collectExplicitMovements(
        project: Project,
        beforePaths: List<FilePath>,
        afterPaths: List<FilePath>
    ): Collection<Movement> {
        val movedChanges = ArrayList<Movement>()
        for (after in afterPaths) {
            val pathBeforeJ2K = after.virtualFile?.pathBeforeJ2K
            if (pathBeforeJ2K != null) {
                val before = beforePaths.firstOrNull { it.path == pathBeforeJ2K }
                if (before != null) {
                    movedChanges.add(Movement(before, after))
                }
            }
        }

        return movedChanges
    }

    override fun afterMovementsCommitted(project: Project, movedPaths: MutableList<Couple<FilePath>>) {
        movedPaths.forEach { it.second.virtualFile?.pathBeforeJ2K = null }
    }
}
