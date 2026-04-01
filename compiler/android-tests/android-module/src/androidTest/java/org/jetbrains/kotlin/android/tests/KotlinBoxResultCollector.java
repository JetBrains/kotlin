/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests;

import android.util.Base64;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class KotlinBoxResultCollector {
    public static final String RESULT_BEGIN = "KOTLIN_BOX_RESULTS_BEGIN";
    public static final String RESULT_END = "KOTLIN_BOX_RESULTS_END";
    public static final String CASE_PREFIX = "KOTLIN_BOX_CASE|";
    public static final String STATUS_OK = "OK";
    public static final String STATUS_FAIL = "FAIL";

    private final List<Entry> entries = new ArrayList<>();

    public void recordSuccess(String name) {
        entries.add(new Entry(name, STATUS_OK, null));
    }

    public void recordFailure(String name, String context, Throwable throwable) {
        entries.add(new Entry(name, STATUS_FAIL, encodeFailure(context, throwable)));
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(RESULT_BEGIN).append('\n');
        for (Entry entry : entries) {
            sb.append(CASE_PREFIX).append(entry.name).append('|').append(entry.status);
            if (entry.failurePayloadBase64 != null) {
                sb.append('|').append(entry.failurePayloadBase64);
            }
            sb.append('\n');
        }
        sb.append(RESULT_END).append('\n');
        return sb.toString();
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

    private static final class Entry {
        private final String name;
        private final String status;
        private final String failurePayloadBase64;

        private Entry(String name, String status, String failurePayloadBase64) {
            this.name = name;
            this.status = status;
            this.failurePayloadBase64 = failurePayloadBase64;
        }
    }
}
