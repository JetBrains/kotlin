/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.descriptors.propertyIfAccessor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker

internal val DeclarationDescriptor.isExpectMember: Boolean
    get() = this is MemberDescriptor && this.isExpect

internal val DeclarationDescriptor.isSerializableExpectClass: Boolean
    get() = this is ClassDescriptor && ExpectedActualDeclarationChecker.shouldGenerateExpectClass(this)

tailrec internal fun DeclarationDescriptor.findPackage(): PackageFragmentDescriptor {
    return if (this is PackageFragmentDescriptor) this
    else this.containingDeclaration!!.findPackage()
}

fun DeclarationDescriptor.findTopLevelDescriptor(): DeclarationDescriptor {
    return if (this.containingDeclaration is org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor) this.propertyIfAccessor
    else this.containingDeclaration!!.findTopLevelDescriptor()
}