/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols.objc

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.VirtualFileOwner
import org.jetbrains.konan.resolve.symbols.KtLazySymbol
import org.jetbrains.konan.resolve.translation.KtOCSymbolTranslator
import org.jetbrains.konan.resolve.translation.StubAndProject
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel

abstract class KtOCLazySymbol<State : KtLazySymbol.StubState, Stb : ObjCTopLevel<*>> : KtLazySymbol<State, Stb>, VirtualFileOwner {
    @Transient
    private lateinit var file: VirtualFile

    constructor(stubAndProject: StubAndProject<Stb>, file: VirtualFile)
            : super(stubAndProject, stubAndProject.stub.name) {
        this.file = file
    }

    constructor() : super()

    override fun getContainingFile(): VirtualFile = file

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean {
        if (!super.deepEqualStep(c, first, second)) return false

        val f = first as KtOCLazySymbol<*, *>
        val s = second as KtOCLazySymbol<*, *>

        if (!Comparing.equal(f.file, s.file)) return false

        return true
    }

    override fun hashCodeExcludingOffset(): Int = name.hashCode() * 31 + file.hashCode()

    override fun init(file: VirtualFile) {
        this.file = file
    }

    companion object {
        @JvmStatic
        protected fun createTranslator(project: Project): KtOCSymbolTranslator = KtOCSymbolTranslator(project)
    }
}
