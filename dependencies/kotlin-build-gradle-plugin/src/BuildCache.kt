/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.initialization.Settings
import org.gradle.caching.http.HttpBuildCache
import java.net.URI


fun Settings.setupBuildCache() {
    buildCache.local.isEnabled = kotlinBuildProperties.localBuildCacheEnabled
    kotlinBuildProperties.buildCacheUrl?.let { remoteCacheUrl ->
        buildCache.remote(HttpBuildCache::class.java) { remoteCache ->
            remoteCache.url = URI(remoteCacheUrl)
            remoteCache.isPush = kotlinBuildProperties.pushToBuildCache
        }
    }
}