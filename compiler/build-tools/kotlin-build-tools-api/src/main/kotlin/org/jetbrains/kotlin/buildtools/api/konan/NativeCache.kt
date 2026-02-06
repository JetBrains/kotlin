/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan

import java.nio.file.Path

public sealed interface NativeCache {
    public val klib: Path
    public val cache: Path

    public class Monolithic(public override val klib: Path, public override val cache: Path) : NativeCache
    public class PerFile(public override val klib: Path, public override val cache: Path) : NativeCache
}