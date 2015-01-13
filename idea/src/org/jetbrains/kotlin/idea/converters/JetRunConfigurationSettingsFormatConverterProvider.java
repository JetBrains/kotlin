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

package org.jetbrains.kotlin.idea.converters;

import com.intellij.conversion.*;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JetRunConfigurationSettingsFormatConverterProvider extends ConverterProvider {
    protected JetRunConfigurationSettingsFormatConverterProvider() {
        super("kotlin-run-configuration-should-store-its-settings-as-java-run-configuration");
    }

    @NotNull
    @Override
    public String getConversionDescription() {
        return "Kotlin run configurations settings will be converted to Java run configurations format";
    }

    @NotNull
    @Override
    public ProjectConverter createConverter(@NotNull ConversionContext context) {
        return new ProjectConverter() {
            @NotNull
            @Override
            public ConversionProcessor<RunManagerSettings> createRunConfigurationsConverter() {
                return new ConversionProcessor<RunManagerSettings>() {
                    @Override
                    public boolean isConversionNeeded(@NotNull RunManagerSettings settings) {
                        for (Element runConfiguration : settings.getRunConfigurations()) {
                            if (isJetRunConfiguration(runConfiguration) && getOldSettings(runConfiguration) != null) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public void process(@NotNull RunManagerSettings settings) throws CannotConvertException {
                        for (Element runConfiguration : settings.getRunConfigurations()) {
                            if (isJetRunConfiguration(runConfiguration)) {
                                Element oldSettings = getOldSettings(runConfiguration);
                                if (oldSettings == null) continue;
                                for (Object optionObj : oldSettings.getChildren("option")) {
                                    if (optionObj instanceof Element) {
                                        Element option = (Element)optionObj;
                                        String optionName = option.getAttributeValue("name");
                                        String optionValue = option.getAttributeValue("value");
                                        if ("mainClassName".equals(optionName)) {
                                            runConfiguration.addContent(createOption("MAIN_CLASS_NAME", optionValue));
                                        }
                                        else if ("programParameters".equals(optionName)) {
                                            runConfiguration.addContent(createOption("PROGRAM_PARAMETERS", optionValue));
                                        }
                                        else if ("vmParameters".equals(optionName)) {
                                            runConfiguration.addContent(createOption("VM_PARAMETERS", optionValue));
                                        }
                                        else if ("workingDirectory".equals(optionName)) {
                                            runConfiguration.addContent(createOption("WORKING_DIRECTORY", optionValue));
                                        }
                                    }
                                }
                                runConfiguration.removeContent(oldSettings);
                            }
                        }
                    }
                };
            }
        };
    }

    private static boolean isJetRunConfiguration(@NotNull Element runConfiguration) {
        return "JetRunConfigurationType".equals(runConfiguration.getAttributeValue("type"));
    }

    @Nullable
    private static Element getOldSettings(@NotNull Element runConfiguration) {
        return runConfiguration.getChild("JetRunConfigurationSettings");
    }

    @NotNull
    private static Element createOption(@NotNull String name, @Nullable String value) {
        Element option = new Element("option");
        option.setAttribute("name", name);
        option.setAttribute("value", value == null ? "" : value);
        return option;
    }
}
