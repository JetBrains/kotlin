/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.name.ClassId

interface KtClassLikeDeclaration : KtNamedDeclaration {
    /**
     * Return [ClassId], if the class is not local (E.e, if a class can be accessed by that [ClassId] from another context)
     *
     * For classes that itself local (are declared inside a function or other local scope), returns `null`.
     * For nested classes in local classes returns `null`.
     * For KtEnumEntry returns null as enum entry is not a class semantically. And so, for nested classes in enum entry, returns `null`.
     * Otherwise, returns non-null [ClassId].
     *
     * For returned ClassId, the [ClassId.isLocal] is always `false`.
     */
    fun getClassId(): ClassId?
}