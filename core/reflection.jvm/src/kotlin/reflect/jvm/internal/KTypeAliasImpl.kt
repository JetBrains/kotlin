/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.name.FqName
import kotlin.reflect.KClassifier

internal class KTypeAliasImpl(val fqName: FqName) : KClassifier, KClassifierImpl {
    override val descriptor: ClassifierDescriptor
        get() = error("Cannot load descriptor of a type alias: $fqName")
}
