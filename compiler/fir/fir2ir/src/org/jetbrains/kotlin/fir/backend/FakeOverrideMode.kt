/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

enum class FakeOverrideMode {
    // No fake overrides are generated
    NONE,
    // Only fake overrides with substituted signatures are generated
    SUBSTITUTION,
    // All fake overrides are generated, including trivial ones, explicitly declared function is specified as overridden one
    NORMAL,
    // All fake overrides are generated, function of direct base class (possibly also fake override) is specified as overridden one
    // TODO: not supported yet (to be discussed)
    FULLY_COMPATIBLE
}