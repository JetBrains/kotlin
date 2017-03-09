/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.compiler.configuration;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.text.VersionComparatorUtil;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.config.FacetSerializationKt;
import org.jetbrains.kotlin.config.LanguageVersion;

import static org.jetbrains.kotlin.config.SettingConstants.KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION;

@State(
    name = KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION,
    storages = {
        @Storage(file = StoragePathMacros.PROJECT_FILE),
        @Storage(file = BaseKotlinCompilerSettings.KOTLIN_COMPILER_SETTINGS_PATH, scheme = StorageScheme.DIRECTORY_BASED)
    }
)
public class KotlinCommonCompilerArgumentsHolder extends BaseKotlinCompilerSettings<CommonCompilerArguments> {
    private static String DEFAULT_LANGUAGE_VERSION = LanguageVersion.LATEST.getVersionString();

    public static KotlinCommonCompilerArgumentsHolder getInstance(Project project) {
        return ServiceManager.getService(project, KotlinCommonCompilerArgumentsHolder.class);
    }

    private static void dropElementIfDefault(@Nullable Element element) {
        if (element == null) return;

        Attribute versionAttribute = element.getAttribute("value");
        String version = versionAttribute != null ? versionAttribute.getValue() : null;
        if (DEFAULT_LANGUAGE_VERSION.equals(version)) {
            element.detach();
        }
    }

    @Override
    public Element getState() {
        Element element = super.getState();
        if (element != null) {
            // Do not serialize language/api version if they correspond to the default language version
            dropElementIfDefault(FacetSerializationKt.getOption(element, "languageVersion"));
            dropElementIfDefault(FacetSerializationKt.getOption(element, "apiVersion"));
        }
        return element;
    }

    @Override
    public void loadState(Element state) {
        super.loadState(state);

        // To fix earlier configurations with incorrect combination of language and API version
        CommonCompilerArguments settings = getSettings();
        if (VersionComparatorUtil.compare(settings.languageVersion, settings.apiVersion) < 0) {
            settings.apiVersion = settings.languageVersion;
        }
    }

    @NotNull
    @Override
    protected CommonCompilerArguments createSettings() {
        return CommonCompilerArguments.createDefaultInstance();
    }
}
