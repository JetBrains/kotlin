/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.tmp

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.types.KotlinType

interface NativeElementsFactory {
    fun convertToRDumpDescriptor(descriptor: DeclarationDescriptor): RDumpDescriptor
    fun convertToRDumpType(type: KotlinType): RDumpType
    fun convertToRDumpDiagnostic(diagnostic: Diagnostic): RDumpDiagnostic
}
