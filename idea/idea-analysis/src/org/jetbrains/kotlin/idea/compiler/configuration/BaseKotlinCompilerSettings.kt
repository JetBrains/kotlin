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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.config.SettingConstants;

import static com.intellij.openapi.components.StoragePathMacros.PROJECT_CONFIG_DIR;

public abstract class BaseKotlinCompilerSettings<T> implements PersistentStateComponent<Element> {
    public static final String KOTLIN_COMPILER_SETTINGS_PATH = PROJECT_CONFIG_DIR + "/" + SettingConstants.KOTLIN_COMPILER_SETTINGS_FILE;

    private static final SkipDefaultValuesSerializationFilters SKIP_DEFAULT_VALUES = new SkipDefaultValuesSerializationFilters(
            CommonCompilerArguments.createDefaultInstance(),
            K2JVMCompilerArguments.createDefaultInstance(),
            K2JSCompilerArguments.createDefaultInstance()
    );
    @NotNull
    private T settings;

    protected BaseKotlinCompilerSettings() {
        //noinspection AbstractMethodCallInConstructor
        this.settings = createSettings();
    }

    @NotNull
    public T getSettings() {
        return settings;
    }

    @NotNull
    protected abstract T createSettings();

    @Override
    public Element getState() {
        return XmlSerializer.serialize(settings, SKIP_DEFAULT_VALUES);
    }

    @Override
    public void loadState(Element state) {
        //noinspection unchecked
        T newSettings = (T) XmlSerializer.deserialize(state, settings.getClass());
        if (newSettings == null)
            newSettings = createSettings();

        settings = newSettings;
    }

    @Override
    @Nullable
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
