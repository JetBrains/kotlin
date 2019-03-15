/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.util.messages.Topic

interface KotlinRefactoringEventListener {
    companion object {
        val EVENT_TOPIC = Topic.create("KOTLIN_REFACTORING_EVENT_TOPIC", KotlinRefactoringEventListener::class.java)
    }

    fun onRefactoringExit(refactoringId: String)
}