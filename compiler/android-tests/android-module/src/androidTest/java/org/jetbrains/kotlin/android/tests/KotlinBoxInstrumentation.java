/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;

public class KotlinBoxInstrumentation extends Instrumentation {
    private static final String ARG_CLASS = "class";
    private static final int STATUS_CODE_IN_PROGRESS = 0;
    private Bundle startArguments;

    @Override
    public void onCreate(Bundle arguments) {
        startArguments = arguments;
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle args = startArguments;
        String suiteClass = args != null ? args.getString(ARG_CLASS) : null;

        KotlinBoxResultCollector collector = new KotlinBoxResultCollector(new StatusReporter(this));
        int resultCode = Activity.RESULT_OK;

        try {
            if (suiteClass == null || suiteClass.isEmpty()) {
                throw new IllegalArgumentException("Missing instrumentation argument: class=<suite fq name>");
            }

            Class<?> klass = Class.forName(suiteClass);
            KotlinBoxTestSuite suite = (KotlinBoxTestSuite) klass.getDeclaredConstructor().newInstance();
            suite.run(collector);
        } catch (Throwable t) {
            collector.recordFailure("initializationError", "Suite: " + suiteClass, t);
            resultCode = Activity.RESULT_CANCELED;
        }

        finish(resultCode, new Bundle());
    }

    private static final class StatusReporter implements KotlinBoxResultCollector.Listener {
        private final KotlinBoxInstrumentation instrumentation;

        private StatusReporter(KotlinBoxInstrumentation instrumentation) {
            this.instrumentation = instrumentation;
        }

        @Override
        public void testStarted(String name) {
            Bundle status = new Bundle();
            status.putString(KotlinBoxResultCollector.EVENT_KEY, KotlinBoxResultCollector.EVENT_STARTED);
            status.putString(KotlinBoxResultCollector.TEST_NAME_KEY, name);
            instrumentation.sendStatus(STATUS_CODE_IN_PROGRESS, status);
        }

        @Override
        public void testFinished(String name, String statusText, long elapsedTimeMs, String failurePayloadBase64) {
            Bundle status = new Bundle();
            status.putString(KotlinBoxResultCollector.EVENT_KEY, KotlinBoxResultCollector.EVENT_FINISHED);
            status.putString(KotlinBoxResultCollector.TEST_NAME_KEY, name);
            status.putString(KotlinBoxResultCollector.STATUS_KEY, statusText);
            status.putString(KotlinBoxResultCollector.ELAPSED_TIME_MS_KEY, Long.toString(elapsedTimeMs));
            if (failurePayloadBase64 != null) {
                status.putString(KotlinBoxResultCollector.FAILURE_PAYLOAD_KEY, failurePayloadBase64);
            }
            instrumentation.sendStatus(STATUS_CODE_IN_PROGRESS, status);
        }
    }
}
