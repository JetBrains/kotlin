/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.intention

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile


val CREATE_XML_RESOURCE_PARAMETERS_NAME_KEY = Key<String>("CREATE_XML_RESOURCE_PARAMETERS_NAME_KEY")

class CreateXmlResourceParameters(
    val name: String,
    val value: String,
    val fileName: String,
    val resourceDirectory: VirtualFile,
    val directoryNames: List<String>
)