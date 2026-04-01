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

        KotlinBoxResultCollector collector = new KotlinBoxResultCollector();
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

        Bundle result = new Bundle();
        result.putString(REPORT_KEY_STREAMRESULT, collector.render());
        finish(resultCode, result);
    }
}
