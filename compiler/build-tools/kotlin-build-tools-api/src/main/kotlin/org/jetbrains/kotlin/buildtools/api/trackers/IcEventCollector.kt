/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.trackers

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * Tracker interface to be implemented by any consumer to forward IC events in the form of IcEvent objects
 * from BTA-impl and the compiler up to their caller.
 */
@ExperimentalBuildToolsApi
public interface IcEventCollector {
    public fun collectEvents(eventsFromBta: List<IcEvent>)
}
