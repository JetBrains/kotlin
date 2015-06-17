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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.KotlinCodeBlockModificationListener

// NOTE: Sadly we must track out of block completion and drop sessions for synthetic files after the completion happens.
// Synthetic file for completion can be modified without sending tree changed events and sequence of completions can lead to inconsistent
// resolve session being cached for such a file otherwise.
// This code is not tested. See KT-6216 for an example.
public class KotlinOutOfBlockCompletionModificationTracker() : SimpleModificationTracker() {
    companion object {
        public fun getInstance(project: Project): KotlinOutOfBlockCompletionModificationTracker
                = ServiceManager.getService(project, javaClass<KotlinOutOfBlockCompletionModificationTracker>())!!
    }
}


public fun performCompletionWithOutOfBlockTracking(completionPosition: PsiElement, body: () -> Unit) {
    if (KotlinCodeBlockModificationListener.isInsideCodeBlock(completionPosition)) {
        body()
        return
    }
    try {
        body()
    }
    finally {
        KotlinOutOfBlockCompletionModificationTracker.getInstance(completionPosition.getProject()).incModificationCount()
    }
}