/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.descriptors.propertyIfAccessor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

internal val DeclarationDescriptor.isExpectMember: Boolean
    get() = this is MemberDescriptor && this.isExpect

internal val DeclarationDescriptor.isSerializableExpectClass: Boolean
    get() = this is ClassDescriptor && ExpectedActualDeclarationChecker.shouldGenerateExpectClass(this)

fun <T> AnnotationDescriptor.getArgumentValueOrNull(name: String): T? {
    val constantValue = this.allValueArguments.entries.atMostOne {
        it.key.asString() == name
    }?.value
    return constantValue?.value as T?
}

private val symbolNameAnnotation = FqName("kotlin.native.SymbolName")

fun getAnnotationValue(annotation: AnnotationDescriptor): String? {
    return annotation.allValueArguments.values.ifNotEmpty {
        val stringValue = single() as? StringValue
        stringValue?.value
    }
}

fun CallableMemberDescriptor.externalSymbolOrThrow(): String? {
    this.annotations.findAnnotation(symbolNameAnnotation)?.let {
        return getAnnotationValue(it)!!
    }

    throw Error("external function ${this} must have @TypedIntrinsic, @SymbolName or @ObjCMethod annotation")
}

tailrec internal fun DeclarationDescriptor.findPackage(): PackageFragmentDescriptor {
    return if (this is PackageFragmentDescriptor) this
    else this.containingDeclaration!!.findPackage()
}

fun DeclarationDescriptor.findTopLevelDescriptor(): DeclarationDescriptor {
    return if (this.containingDeclaration is org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor) this.propertyIfAccessor
    else this.containingDeclaration!!.findTopLevelDescriptor()
}