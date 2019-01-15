/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

// Any target of return / break / continue (some targets may have labels, some never have them)
interface FirTargetElement : FirElement