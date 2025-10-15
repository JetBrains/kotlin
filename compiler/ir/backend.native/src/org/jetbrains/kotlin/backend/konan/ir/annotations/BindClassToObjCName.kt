/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir.annotations

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.getAnnotationStringValue
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeRuntimeNames

/**
 * A representation of `@BindClassToObjCName` annotation.
 */
class BindClassToObjCName(
        val annotationElement: IrConstructorCall,
        val kotlinClass: IrClass,
        val objCName: String,
)

private val IrConstructorCall.bindClassToObjCName: BindClassToObjCName?
    get() {
        if (!isAnnotation(NativeRuntimeNames.Annotations.BindClassToObjCName.asSingleFqName())) {
            return null
        }
        val kotlinClass = (getValueArgument(Name.identifier("kotlinClass")) as IrClassReference).classType.getClass()!!
        val objCName = getAnnotationStringValue("objCName")
        return BindClassToObjCName(this, kotlinClass, objCName)
    }

/**
 * Return a list of `@BindClassToObjCName` annotations attached to this [IrFile].
 */
val IrFile.allBindClassToObjCName: List<BindClassToObjCName>
    get() = annotations.mapNotNull { it.bindClassToObjCName }