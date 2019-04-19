/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier

class ValueParameter(val isVal: Boolean, val isVar: Boolean, val modifier: Modifier, val firValueParameter: FirValueParameter)