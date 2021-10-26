/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol

private object DelegateFieldsMapKey : FirDeclarationDataKey()

/*
 * If class implements some interfaces using delegation then this attribute contains mapping
 *   from index of supertype to symbol of deelgated field
 */
var FirClass.delegateFieldsMap: Map<Int, FirFieldSymbol>? by FirDeclarationDataRegistry.data(DelegateFieldsMapKey)

