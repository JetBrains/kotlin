/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

/**
 * @param annotations list of module annotations, in the format: "org/foo/bar/Baz.Inner" (see [ClassId.fromString])
 */
class BinaryModuleData(val annotations: List<String>)
