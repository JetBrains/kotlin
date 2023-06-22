/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.common.arguments;

public interface K2JsArgumentConstants {
    String CALL = "call";
    String NO_CALL = "noCall";

    String MODULE_PLAIN = "plain";
    String MODULE_AMD = "amd";
    String MODULE_COMMONJS = "commonjs";
    String MODULE_UMD = "umd";
    String MODULE_ES = "es";

    String GRANULARITY_WHOLE_PROGRAM = "whole-program";
    String GRANULARITY_PER_MODULE = "per-module";
    String GRANULARITY_PER_FILE = "per-file";

    String SOURCE_MAP_SOURCE_CONTENT_ALWAYS = "always";
    String SOURCE_MAP_SOURCE_CONTENT_NEVER = "never";
    String SOURCE_MAP_SOURCE_CONTENT_INLINING = "inlining";

    String SOURCE_MAP_NAMES_POLICY_NO = "no";
    String SOURCE_MAP_NAMES_POLICY_SIMPLE_NAMES = "simple-names";
    String SOURCE_MAP_NAMES_POLICY_FQ_NAMES = "fully-qualified-names";

    String RUNTIME_DIAGNOSTIC_LOG = "log";
    String RUNTIME_DIAGNOSTIC_EXCEPTION = "exception";
}
