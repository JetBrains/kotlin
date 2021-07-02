/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.execution;

import com.intellij.openapi.util.NlsContexts;

public class IllegalEnvVarException extends ExecutionException {
    public IllegalEnvVarException(String message) {
        super(message);
    }
}
