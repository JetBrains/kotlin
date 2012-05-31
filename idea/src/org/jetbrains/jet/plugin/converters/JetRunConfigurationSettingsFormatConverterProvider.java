/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.converters;

import com.intellij.conversion.*;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maxim.Manuylov
 *         Date: 29.05.12
 */
public class JetRunConfigurationSettingsFormatConverterProvider extends ConverterProvider {
    protected JetRunConfigurationSettingsFormatConverterProvider() {
        super("jet-run-configuration-should-store-its-settings-as-java-app-configuration");
    }

    @NotNull
    @Override
    public String getConversionDescription() {
        return "Kotlin run configurations settings will be converted to Java run configurations format";
    }

    @NotNull
    @Override
    public ProjectConverter createConverter(@NotNull final ConversionContext context) {
        return new ProjectConverter() {
            @NotNull
            @Override
            public ConversionProcessor<RunManagerSettings> createRunConfigurationsConverter() {
                return new ConversionProcessor<RunManagerSettings>() {
                    @Override
                    public boolean isConversionNeeded(@NotNull final RunManagerSettings settings) {
                        for (final Element runConfiguration : settings.getRunConfigurations()) {
                            if (isJetRunConfiguration(runConfiguration) && getOldSettings(runConfiguration) != null) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public void process(@NotNull final RunManagerSettings settings) throws CannotConvertException {
                        for (final Element runConfiguration : settings.getRunConfigurations()) {
                            if (isJetRunConfiguration(runConfiguration)) {
                                final Element oldSettings = getOldSettings(runConfiguration);
                                if (oldSettings == null) continue;
                                for (final Object optionObj : oldSettings.getChildren("option")) {
                                    if (optionObj instanceof Element) {
                                        final Element option = (Element)optionObj;
                                        final String optionName = option.getAttributeValue("name");
                                        final String optionValue = option.getAttributeValue("value");
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

    private static boolean isJetRunConfiguration(@NotNull final Element runConfiguration) {
        return "JetRunConfigurationType".equals(runConfiguration.getAttributeValue("type"));
    }

    @Nullable
    private static Element getOldSettings(@NotNull final Element runConfiguration) {
        return runConfiguration.getChild("JetRunConfigurationSettings");
    }

    @NotNull
    private static Element createOption(@NotNull final String name, @Nullable final String value) {
        final Element option = new Element("option");
        option.setAttribute("name", name);
        option.setAttribute("value", value == null ? "" : value);
        return option;
    }
}
