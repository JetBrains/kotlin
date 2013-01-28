/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ShutDownTracker;

public final class ApplicationUtils {
    private ApplicationUtils() {
    }

    public static void warnTimeConsuming(Logger logger) {
        Application application = ApplicationManager.getApplication();
        if (application.isUnitTestMode() || application.isHeadlessEnvironment() || ShutDownTracker.isShutdownHookRunning()) {
            return;
        }

        if (!application.isDispatchThread()) {
            return;
        }

        logger.warn("This operation is time consuming and must not be called on EDT.");

        //noinspection CallToPrintStackTrace
        new Throwable().printStackTrace();
    }
}
