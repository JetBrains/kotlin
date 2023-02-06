/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config;

import org.jetbrains.kotlin.config.CompilerConfigurationKey;
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider;
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker;
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer;

public class WebConfigurationKeys {
    private WebConfigurationKeys() {
    }

    public static final CompilerConfigurationKey<IncrementalDataProvider> INCREMENTAL_DATA_PROVIDER =
            CompilerConfigurationKey.create("incremental data provider");

    public static final CompilerConfigurationKey<IncrementalResultsConsumer> INCREMENTAL_RESULTS_CONSUMER =
            CompilerConfigurationKey.create("incremental results consumer");

    public static final CompilerConfigurationKey<IncrementalNextRoundChecker> INCREMENTAL_NEXT_ROUND_CHECKER =
            CompilerConfigurationKey.create("incremental compilation next round checker");

    public static final CompilerConfigurationKey<Boolean> PRINT_REACHABILITY_INFO =
            CompilerConfigurationKey.create("print declarations' reachability info during performing DCE");

    public static final CompilerConfigurationKey<Boolean> FAKE_OVERRIDE_VALIDATOR =
            CompilerConfigurationKey.create("IR fake override validator");

    public static final CompilerConfigurationKey<Boolean> PROPERTY_LAZY_INITIALIZATION =
            CompilerConfigurationKey.create("perform lazy initialization for properties");

    public static final CompilerConfigurationKey<ErrorTolerancePolicy> ERROR_TOLERANCE_POLICY =
            CompilerConfigurationKey.create("set up policy to ignore compilation errors");

    public static final CompilerConfigurationKey<Boolean> PARTIAL_LINKAGE =
            CompilerConfigurationKey.create("allows some symbols in klibs be missed");
}
