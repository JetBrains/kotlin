/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module

fun Module.isAndroidModule(): Boolean {
    val facets = FacetManager.getInstance(this).allFacets
    return facets.any { it.javaClass.simpleName == "AndroidFacet" }
}