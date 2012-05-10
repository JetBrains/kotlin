/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.reporter;

import com.intellij.diagnostic.ITNReporter;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;

import java.awt.*;

/**
 * We need to wrap ITNReporter into this delegating class to work around the following problem:
 *
 * Kotlin's lifecycle does not align with the one of IDEA, so every now and then we are in the situation when users
 * install an unstable build of Kotlin on a released build of IDEA. Since IDEA does not show exceptions from ITNReporter
 * in release build, the user doesn't see exceptions from Kotlin in such a situations. Wrapping solves this problem:
 * even release builds of IDEA will report Kotlin exceptions to the user (and allow to submit them to Exception Analyzer).
 *
 * @author abreslav
 */
public class KotlinReportSubmitter extends ErrorReportSubmitter {

    private final ErrorReportSubmitter delegate = new ITNReporter();

    @Override
    public String getReportActionText() {
        return delegate.getReportActionText();
    }

    @Override
    public SubmittedReportInfo submit(IdeaLoggingEvent[] events, Component parentComponent) {
        return delegate.submit(events, parentComponent);
    }
}
