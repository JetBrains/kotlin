/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.util.xmlb.annotations.Attribute
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties

class KotlinPropertyBreakpointProperties(
    @Attribute var myFieldName: String = "",
    @Attribute var myClassName: String = ""
) : JavaBreakpointProperties<KotlinPropertyBreakpointProperties>() {
    var watchModification: Boolean = true
    var watchAccess: Boolean = false
    var watchInitialization: Boolean = false

    override fun getState() = this

    override fun loadState(state: KotlinPropertyBreakpointProperties) {
        super.loadState(state)

        watchModification = state.watchModification
        watchAccess = state.watchAccess
        watchInitialization = state.watchInitialization

        myFieldName = state.myFieldName
        myClassName = state.myClassName
    }
}