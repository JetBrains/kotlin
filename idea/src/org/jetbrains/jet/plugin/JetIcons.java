/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.PlatformIcons;

import javax.swing.*;

public interface JetIcons {
    Icon SMALL_LOGO = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/kotlin.png");
    Icon KOTLIN_LOGO_24 = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/kotlin24.png");

    Icon SMALL_LOGO_13 = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/kotlin13.png");

    Icon CLASS = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/classKotlin.png");
    Icon ENUM = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/enumKotlin.png");
    Icon FILE = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/kotlin_file.png");
    Icon OBJECT = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/objectKotlin.png");
    Icon TRAIT = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/traitKotlin.png");
    Icon FUNCTION = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/function.png");
    Icon EXTENSION_FUNCTION = PlatformIcons.FUNCTION_ICON;
    Icon LAMBDA = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/lambda.png");
    Icon VAR = PlatformIcons.VARIABLE_ICON;
    Icon VAL = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/value.png");
    Icon PARAMETER = PlatformIcons.PARAMETER_ICON;
    Icon FIELD_VAL = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/field_value.png");
    Icon FIELD_VAR = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/field_variable.png");

    Icon LAUNCH = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/kotlin_launch_configuration.png");
}
