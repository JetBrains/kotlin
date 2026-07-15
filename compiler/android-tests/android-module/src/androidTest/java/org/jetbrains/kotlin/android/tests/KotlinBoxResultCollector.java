/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests;

import android.util.Base64;

import java.io.PrintWriter;
import java.io.StringWriter;

public class KotlinBoxResultCollector {
    public static final String STATUS_OK = "OK";
    public static final String STATUS_FAIL = "FAIL";
    public static final String EVENT_KEY = "kotlinBoxEvent";
    public static final String EVENT_STARTED = "started";
    public static final String EVENT_FINISHED = "finished";
    public static final String TEST_NAME_KEY = "testName";
    public static final String STATUS_KEY = "status";
    public static final String ELAPSED_TIME_MS_KEY = "elapsedTimeMs";
    public static final String FAILURE_PAYLOAD_KEY = "failurePayloadBase64";

    private final Listener listener;

    public KotlinBoxResultCollector(Listener listener) {
        this.listener = listener;
    }

    public void recordStart(String name) {
        listener.testStarted(name);
    }

    public void recordSuccess(String name) {
        recordSuccess(name, -1L);
    }

    public void recordSuccess(String name, long elapsedTimeMs) {
        listener.testFinished(name, STATUS_OK, elapsedTimeMs, null);
    }

    public void recordFailure(String name, String context, Throwable throwable) {
        recordFailure(name, context, throwable, -1L);
    }

    public void recordFailure(String name, String context, Throwable throwable, long elapsedTimeMs) {
        listener.testFinished(name, STATUS_FAIL, elapsedTimeMs, encodeFailure(context, throwable));
    }

    private static String encodeFailure(String context, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (context != null && !context.isEmpty()) {
            pw.println(context);
        }
        throwable.printStackTrace(pw);
        pw.flush();
        return Base64.encodeToString(sw.toString().getBytes(), Base64.NO_WRAP);
    }

    public interface Listener {
        void testStarted(String name);

        void testFinished(String name, String status, long elapsedTimeMs, String failurePayloadBase64);
    }
}
