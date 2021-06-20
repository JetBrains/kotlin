/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.metadata.deserialization.VersionRequirementTable

object FirVersionRequirementsTableKey : FirDeclarationDataKey()

var FirDeclaration<*>.versionRequirementsTable: VersionRequirementTable? by FirDeclarationDataRegistry.data(FirVersionRequirementsTableKey)
