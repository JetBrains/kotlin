/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.refactoring.move.MoveHandlerDelegate
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler

// BUNCH: 191
typealias MoveHandlerDelegateCompat = MoveHandlerDelegate
// BUNCH: 191
typealias MoveFilesOrDirectoriesHandlerCompat = MoveFilesOrDirectoriesHandler