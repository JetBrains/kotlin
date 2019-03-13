/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins;

import org.jetbrains.kotlin.container.PlatformExtensionsClashResolver;

public class PlatformToKotlinClassMapClashesResolver extends PlatformExtensionsClashResolver.UseAnyOf<PlatformToKotlinClassMap> {
    public PlatformToKotlinClassMapClashesResolver() {
        super(PlatformToKotlinClassMap.EMPTY, PlatformToKotlinClassMap.class);
    }
}
