/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.hasExcludedFromCodegenAnnotation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqName

private val BODILESS_BUILTIN_CLASSES = listOf(
    "kotlin.Nothing",
    "kotlin.Array",
    "kotlin.Any",
    "kotlin.ByteArray",
    "kotlin.CharArray",
    "kotlin.ShortArray",
    "kotlin.IntArray",
    "kotlin.LongArray",
    "kotlin.FloatArray",
    "kotlin.DoubleArray",
    "kotlin.BooleanArray",
    "kotlin.Boolean",
    "kotlin.Function",
    "kotlin.Throwable",
    "kotlin.Suppress",
    "kotlin.SinceKotlin",
    "kotlin.Deprecated",
    "kotlin.ReplaceWith",
    "kotlin.DeprecationLevel",
    "kotlin.UnsafeVariance",
    "kotlin.reflect.KType",
    "kotlin.reflect.KTypeProjection",
    "kotlin.reflect.Companion",
    "kotlin.reflect.KTypeParameter",
    "kotlin.reflect.KDeclarationContainer",
    "kotlin.reflect.KProperty",
    "kotlin.reflect.KProperty0",
    "kotlin.reflect.KProperty1",
    "kotlin.reflect.KProperty2",
    "kotlin.reflect.KMutableProperty0",
    "kotlin.reflect.KMutableProperty",
    "kotlin.reflect.KMutableProperty1",
    "kotlin.reflect.KMutableProperty2",
    "kotlin.reflect.Accessor",
    "kotlin.reflect.Getter",
    "kotlin.reflect.KFunction",
    "kotlin.reflect.KVariance",
    "kotlin.reflect.KVisibility",
    "kotlin.reflect.KClass",
    "kotlin.reflect.KCallable",
    "kotlin.reflect.KClassifier",
    "kotlin.reflect.KParameter",
    "kotlin.reflect.Kind",
    "kotlin.reflect.KAnnotatedElement",
    "kotlin.annotation.Target",
    "kotlin.annotation.AnnotationTarget",
    "kotlin.annotation.Retention",
    "kotlin.annotation.AnnotationRetention",
    "kotlin.annotation.MustBeDocumented",
    "kotlin.Unit",
    "kotlin.collections.BooleanIterator",
    "kotlin.collections.CharIterator",
    "kotlin.collections.ByteIterator",
    "kotlin.collections.ShortIterator",
    "kotlin.collections.IntIterator",
    "kotlin.collections.FloatIterator",
    "kotlin.collections.LongIterator",
    "kotlin.collections.DoubleIterator",
    "kotlin.internal.PlatformDependent",
    "kotlin.CharSequence",
    "kotlin.Annotation",
    "kotlin.Comparable",
    "kotlin.collections.Collection",
    "kotlin.collections.Iterable",
    "kotlin.collections.List",
    "kotlin.collections.Map",
    "kotlin.collections.Set",
    "kotlin.collections.MutableCollection",
    "kotlin.collections.MutableIterable",
    "kotlin.collections.MutableSet",
    "kotlin.collections.MutableList",
    "kotlin.collections.MutableMap",
    "kotlin.collections.Entry",
    "kotlin.collections.MutableEntry",
    "kotlin.Number",
    "kotlin.Enum",
    "kotlin.collections.Iterator",
    "kotlin.collections.ListIterator",
    "kotlin.collections.MutableIterator",
    "kotlin.collections.MutableListIterator"
).map { FqName(it) }.toSet()

fun excludeDeclarationsFromCodegen(context: WasmBackendContext, module: IrModuleFragment) {

    fun isExcluded(declaration: IrDeclaration): Boolean {
        if (declaration is IrDeclarationWithName && declaration.fqNameWhenAvailable in BODILESS_BUILTIN_CLASSES)
            return true

        if (declaration.hasExcludedFromCodegenAnnotation())
            return true

        val parentFile = declaration.parent as? IrFile
        if (parentFile?.hasExcludedFromCodegenAnnotation() == true)
            return true

        return false
    }

    for (file in module.files) {
        val it = file.declarations.iterator()
        while (it.hasNext()) {
            val d = it.next() as? IrDeclarationWithName ?: continue
            if (isExcluded(d)) {
                it.remove()
                context.excludedDeclarations.addChild(d)
            }
        }
    }
}
