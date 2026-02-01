/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.fakes

import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest

/**
 * Base class for all fake test classes used in unit and integration tests.
 * Keeps fake tests separate in the ManagedTest hierarchy.
 */
abstract class FakeManagedTest : ManagedTest
