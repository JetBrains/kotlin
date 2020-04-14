/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope

interface PackageMemberScopeProvider {
    fun getPackageScope(fqName: FqName): MemberScope
    fun getPackageScopeWithoutDependencies(fqName: FqName): MemberScope
}