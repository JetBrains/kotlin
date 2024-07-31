/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import java.util.concurrent.locks.ReentrantLock

object LLSharedCacheLocks {
    /**
     * A [ReentrantLock] which confines FIR Java class computation in post-computing caches to a single thread. Post-computation of FIR Java
     * classes may invoke [FIR signature enhancement][org.jetbrains.kotlin.fir.java.enhancement.FirSignatureEnhancement], which may need to
     * access other FIR Java classes in the same or a different cache. Mutual dependencies between Java classes may then lead to a deadlock.
     *
     * To illustrate this, assume we post-compute two mutually dependent Java classes C1 (in thread T1) and C2 (in thread T2) concurrently.
     * This may lead to the following deadlock:
     *
     *  - T1 tries to access the currently computing value C2, which is only visible to and locked by T2.
     *  - T2 tries to access the currently computing value C1, which is only visible to and locked by T1.
     *
     * As both threads are waiting for each other's resources, we have a deadlock. See [KT-70327](https://youtrack.jetbrains.com/issue/KT-70327)
     * for more information.
     *
     * Confining computation to a single thread is not ideal, and in some cases Java class computation may be a bottleneck. We will consider
     * a different solution long-term.
     *
     * @see org.jetbrains.kotlin.fir.caches.FirCachesFactory.createCacheWithPostCompute
     */
    val sharedJavaClassComputationLock = ReentrantLock()
}
