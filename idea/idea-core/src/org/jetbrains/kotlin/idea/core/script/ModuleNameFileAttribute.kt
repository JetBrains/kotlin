/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.util.cachedFileAttribute
import org.jetbrains.kotlin.idea.core.util.readString
import org.jetbrains.kotlin.idea.core.util.writeString
import java.io.DataInputStream
import java.io.DataOutputStream

var VirtualFile.scriptRelatedModuleName: String? by cachedFileAttribute(
    name = "kotlin-script-moduleName",
    version = 1,
    read = DataInputStream::readString,
    write = DataOutputStream::writeString
)