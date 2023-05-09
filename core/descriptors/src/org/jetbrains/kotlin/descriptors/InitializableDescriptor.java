/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors;

import org.jetbrains.annotations.NotNull;

public interface InitializableDescriptor {

    /**
     * [action] to be executed when descriptor is finalized with initialization.
     * @param action
     */
    void addInitFinalizationAction(@NotNull Runnable action);

    boolean isInitFinalized();
}
