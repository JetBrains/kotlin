/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

enum class FirResolvePhase {
    RAW_FIR,
    ANNOTATIONS_FOR_PLUGINS, // run only if some extensions are registered
    NESTED_CLASS_GENERATION, // plugin phase
    IMPORTS,
    SUPER_TYPES,
    SEALED_CLASS_INHERITORS,
    TYPES,
    EXTENSION_STATUS_UPDATE,
    STATUS,
    CONTRACTS,
    NEW_MEMBERS_GENERATION, // plugin
    IMPLICIT_TYPES_BODY_RESOLVE,
    BODY_RESOLVE;

    val requiredToLaunch: FirResolvePhase
        get() = when (this) {
            RAW_FIR -> RAW_FIR
            IMPORTS -> RAW_FIR
            STATUS -> TYPES
            IMPLICIT_TYPES_BODY_RESOLVE, BODY_RESOLVE -> STATUS
            else -> values()[ordinal - 1]
        }

    val next: FirResolvePhase get() = values()[ordinal + 1]

    companion object {
        // Short-cut
        val DECLARATIONS = STATUS
        val ANALYZED_DEPENDENCIES = BODY_RESOLVE
    }
}
