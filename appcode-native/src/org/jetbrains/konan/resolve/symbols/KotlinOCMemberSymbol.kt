/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCMemberSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.Stub

abstract class KotlinOCMemberSymbol<S : Stub<*>>(
    stub: S,
    project: Project,
    file: VirtualFile,
    private val containingClass: OCClassSymbol
) : KotlinOCWrapperSymbol<S>(stub, project, file), OCMemberSymbol {

    override fun isGlobal(): Boolean = false

    override fun getParent(): OCClassSymbol = containingClass
}