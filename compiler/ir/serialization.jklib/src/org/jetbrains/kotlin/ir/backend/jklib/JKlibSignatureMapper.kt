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

  fun isMappedJavaPlatformClass(classId: ClassId): Boolean =
    isMappedJavaPlatformClass(classId.asSingleFqName())

  fun mapJavaToKotlinClassId(fqName: FqName): ClassId? =
    JavaToKotlinClassMap.mapJavaToKotlin(fqName)

  fun mapJavaToKotlinClassId(classId: ClassId): ClassId? =
    mapJavaToKotlinClassId(classId.asSingleFqName())

  fun mapJavaToKotlinSignature(fqName: FqName): IdSignature.CommonSignature? {
    val kotlinClassId = mapJavaToKotlinClassId(fqName) ?: return null
    return IdSignature.CommonSignature(
      packageFqName = kotlinClassId.packageFqName.asString(),
      declarationFqName = kotlinClassId.relativeClassName.asString(),
      id = null,
      mask = 0,
      description = kotlinClassId.asString(),
    )
  }

  fun mapJavaToKotlinSignature(classId: ClassId): IdSignature.CommonSignature? =
    mapJavaToKotlinSignature(classId.asSingleFqName())

  fun mapJavaSignatureToKotlinSignature(idSig: IdSignature): IdSignature {
    if (idSig is IdSignature.CommonSignature) {
      if (!idSig.packageFqName.startsWith("java")) return idSig
      val classId =
        if (idSig.packageFqName.isEmpty()) {
          ClassId.topLevel(FqName(idSig.declarationFqName))
        } else {
          ClassId(FqName(idSig.packageFqName), FqName(idSig.declarationFqName), false)
        }
      val mapped = mapJavaToKotlinSignature(classId)
      if (mapped != null) {
        return mapped
      }
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

  fun getBuiltInClassSymbolForMappedJavaClass(
    classId: ClassId,
    irBuiltIns: IrBuiltIns,
  ): IrClassSymbol? = getBuiltInClassSymbolForMappedJavaClass(classId.asSingleFqName(), irBuiltIns)

  @OptIn(InternalSymbolFinderAPI::class)
  fun getBuiltInClassSymbol(kotlinClassId: ClassId, irBuiltIns: IrBuiltIns): IrClassSymbol? {
    val pkg = kotlinClassId.packageFqName.asString()
    val name = kotlinClassId.relativeClassName.asString()
    return when {
      pkg == "kotlin" ->
        when (name) {
          "Any" -> irBuiltIns.anyClass
          "String" -> irBuiltIns.stringClass
          "CharSequence" -> irBuiltIns.charSequenceClass
          "Throwable" -> irBuiltIns.throwableClass
          "Number" -> irBuiltIns.numberClass
          "Comparable" -> irBuiltIns.comparableClass
          "Boolean" -> irBuiltIns.booleanClass
          "Char" -> irBuiltIns.charClass
          "Byte" -> irBuiltIns.byteClass
          "Short" -> irBuiltIns.shortClass
          "Int" -> irBuiltIns.intClass
          "Long" -> irBuiltIns.longClass
          "Float" -> irBuiltIns.floatClass
          "Double" -> irBuiltIns.doubleClass
          "Function" -> irBuiltIns.functionClass
          "Enum" -> irBuiltIns.enumClass
          "Annotation" -> irBuiltIns.annotationClass
          else -> irBuiltIns.symbolFinder.findClass(kotlinClassId)
        }
      pkg == "kotlin.collections" ->
        when (name) {
          "Iterable" -> irBuiltIns.iterableClass
          "Iterator" -> irBuiltIns.iteratorClass
          "Collection" -> irBuiltIns.collectionClass
          "List" -> irBuiltIns.listClass
          "Set" -> irBuiltIns.setClass
          "ListIterator" -> irBuiltIns.listIteratorClass
          "Map" -> irBuiltIns.mapClass
          "Map.Entry" -> irBuiltIns.mapEntryClass
          "MutableIterable" -> irBuiltIns.mutableIterableClass
          "MutableIterator" -> irBuiltIns.mutableIteratorClass
          "MutableCollection" -> irBuiltIns.mutableCollectionClass
          "MutableList" -> irBuiltIns.mutableListClass
          "MutableSet" -> irBuiltIns.mutableSetClass
          "MutableListIterator" -> irBuiltIns.mutableListIteratorClass
          "MutableMap" -> irBuiltIns.mutableMapClass
          "MutableMap.Entry" -> irBuiltIns.mutableMapEntryClass
          else -> irBuiltIns.symbolFinder.findClass(kotlinClassId)
        }
      pkg == "kotlin.reflect" ->
        when (name) {
          "KClass" -> irBuiltIns.kClassClass
          "KCallable" -> irBuiltIns.kCallableClass
          "KProperty" -> irBuiltIns.kPropertyClass
          else -> irBuiltIns.symbolFinder.findClass(kotlinClassId)
        }
      else -> irBuiltIns.symbolFinder.findClass(kotlinClassId)
    }
  }
}
