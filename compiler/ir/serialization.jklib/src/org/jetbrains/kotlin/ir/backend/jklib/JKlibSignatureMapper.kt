/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jklib

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object JKlibSignatureMapper {

  fun isMappedJavaPlatformClass(fqName: FqName): Boolean =
    JavaToKotlinClassMap.isJavaPlatformClass(fqName)

  fun mapJavaToKotlinClassId(fqName: FqName): ClassId? =
    JavaToKotlinClassMap.mapJavaToKotlin(fqName)

  fun mapJavaToKotlinSignature(kotlinClassId: ClassId): IdSignature.CommonSignature =
    IdSignature.CommonSignature(
      packageFqName = kotlinClassId.packageFqName.asString(),
      declarationFqName = kotlinClassId.relativeClassName.asString(),
      id = null,
      mask = 0,
      description = kotlinClassId.asString(),
    )

  fun mapJavaSignatureToKotlinSignature(idSig: IdSignature): IdSignature {
    if (idSig is IdSignature.CommonSignature) {
      if (!idSig.packageFqName.startsWith("java")) return idSig
      val fqName =
        if (idSig.packageFqName.isEmpty()) {
          FqName(idSig.declarationFqName)
        } else {
          FqName("${idSig.packageFqName}.${idSig.declarationFqName}")
        }
      val kotlinClassId = mapJavaToKotlinClassId(fqName) ?: return idSig
      return mapJavaToKotlinSignature(kotlinClassId)
    }
    return idSig
  }

  fun getBuiltInClassSymbolForMappedJavaClass(
    fqName: FqName,
    irBuiltIns: IrBuiltIns,
  ): IrClassSymbol? {
    val kotlinClassId = mapJavaToKotlinClassId(fqName) ?: return null
    return getBuiltInClassSymbol(kotlinClassId, irBuiltIns)
  }

  @OptIn(InternalSymbolFinderAPI::class)
  fun getBuiltInClassSymbol(kotlinClassId: ClassId, irBuiltIns: IrBuiltIns): IrClassSymbol? {
    return irBuiltIns.symbolFinder.findClass(kotlinClassId)
  }
}
