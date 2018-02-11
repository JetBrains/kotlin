/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.internal.makeBackup

import com.intellij.history.LocalHistory
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileTask
import com.intellij.openapi.util.Key
import java.util.*

val random = Random()

val HISTORY_LABEL_KEY = Key.create<String>("history label")

class MakeBackupCompileTask: CompileTask {
    override fun execute(context: CompileContext?): Boolean {
        val project = context!!.project!!

        val localHistory = LocalHistory.getInstance()!!
        val label = HISTORY_LABEL_PREFIX + Integer.toHexString(random.nextInt())
        localHistory.putSystemLabel(project, label)

        context.compileScope!!.putUserData(HISTORY_LABEL_KEY, label)

        return true
    }

}
