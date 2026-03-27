/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm

public interface HasSnapshotBasedIcOptionsAccessor {
    public operator fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptionsImpl.Option<V>): V
}

public class JvmSnapshotBasedIncrementalCompilationOptionsImpl {
    public class Option<V>
}
