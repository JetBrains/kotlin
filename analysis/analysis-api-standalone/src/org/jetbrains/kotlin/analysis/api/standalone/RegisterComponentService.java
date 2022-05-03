/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone;

import com.intellij.mock.MockProject;
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionService;

class RegisterComponentService {
    static void registerLLFirResolveSessionService(MockProject project) {
        project.registerService(
                LLFirResolveSessionService.class,
                new LLFirResolveSessionService(project)
        );
    }
}
