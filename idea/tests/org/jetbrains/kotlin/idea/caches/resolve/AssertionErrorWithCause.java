/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve;

public class AssertionErrorWithCause extends AssertionError {
    public AssertionErrorWithCause(String detailMessage, Throwable cause) {
        super(detailMessage);

        initCause(cause);
    }
}
