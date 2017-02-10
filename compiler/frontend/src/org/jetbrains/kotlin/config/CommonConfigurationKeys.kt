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

package org.jetbrains.kotlin.config

object CommonConfigurationKeys {
    @JvmField
    val LANGUAGE_VERSION_SETTINGS = CompilerConfigurationKey<LanguageVersionSettings>("language version settings")

    @JvmField
    val SKIP_METADATA_VERSION_CHECK = CompilerConfigurationKey<Boolean>("skip metadata version check")

    @JvmField
    val DISABLE_INLINE = CompilerConfigurationKey<Boolean>("disable inline")

    @JvmField
    val MODULE_NAME = CompilerConfigurationKey<String>("module name")
}

var CompilerConfiguration.languageVersionSettings: LanguageVersionSettings
    get() = get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
    set(value) = put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)
