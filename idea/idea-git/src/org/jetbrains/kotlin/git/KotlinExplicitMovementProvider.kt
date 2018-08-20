/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.FilePath
import git4idea.checkin.GitCheckinExplicitMovementProvider
import org.jetbrains.kotlin.idea.actions.pathBeforeJ2K
import java.util.*

class KotlinExplicitMovementProvider : GitCheckinExplicitMovementProvider() {
    override fun isEnabled(project: Project): Boolean {
        return true
    }

    override fun getDescription(): String {
        return "Create extra commit with .java -> .kt file renames"
    }

    override fun getCommitMessage(oldCommitMessage: String): String {
        return "Rename .java to .kt"
    }

    override fun collectExplicitMovements(
        project: Project,
        beforePaths: List<FilePath>,
        afterPaths: List<FilePath>
    ): Collection<GitCheckinExplicitMovementProvider.Movement> {
        val movedChanges = ArrayList<GitCheckinExplicitMovementProvider.Movement>()
        for (after in afterPaths) {
            val pathBeforeJ2K = after.virtualFile?.pathBeforeJ2K
            if (pathBeforeJ2K != null) {
                val before = beforePaths.firstOrNull { it.path == pathBeforeJ2K }
                if (before != null) {
                    movedChanges.add(GitCheckinExplicitMovementProvider.Movement(before, after))
                    after.virtualFile?.pathBeforeJ2K = null
                }
            }
        }

        return movedChanges
    }
}
