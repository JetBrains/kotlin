/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.native

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.FirPlatformClassMapper
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.NativeRuntimeNames

class FirNativeClassMapper : FirPlatformClassMapper() {
    override fun getCorrespondingPlatformClass(declaration: FirClassLikeDeclaration): FirRegularClass? {
        return null
    }

    override fun getCorrespondingPlatformClass(classId: ClassId?): ClassId? {
        return null
    }

    override fun getCorrespondingKotlinClass(classId: ClassId?): ClassId? {
        return null
    }

    override val classTypealiasesThatDontCauseAmbiguity: Map<ClassId, ClassId> = mapOf(
        NativeRuntimeNames.Annotations.Throws to NativeRuntimeNames.Annotations.ThrowsAlias,
        NativeRuntimeNames.Annotations.SharedImmutable to NativeRuntimeNames.Annotations.SharedImmutableAlias,
        NativeRuntimeNames.Annotations.ThreadLocal to NativeRuntimeNames.Annotations.ThreadLocalAlias
    )
}