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

package org.jetbrains.kotlin.idea;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.PlatformIcons;

import javax.swing.*;

public interface KotlinIcons {
    Icon SMALL_LOGO = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin.png");
    Icon KOTLIN_LOGO_24 = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin24.png");

    Icon SMALL_LOGO_13 = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin13.png");

    Icon CLASS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/classKotlin.png");
    Icon ABSTRACT_CLASS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/abstractClassKotlin.png");
    Icon ENUM = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/enumKotlin.png");
    Icon FILE = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_file.png");
    Icon ACTIVITY = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_activity.png");
    Icon OBJECT = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/objectKotlin.png");
    Icon INTERFACE = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/interfaceKotlin.png");
    Icon ANNOTATION = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/annotationKotlin.png");
    Icon FUNCTION = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/function.png");
    Icon EXTENSION_FUNCTION = PlatformIcons.FUNCTION_ICON;
    Icon ABSTRACT_EXTENSION_FUNCTION = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/abstract_extension_function.png");
    Icon LAMBDA = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/lambda.png");
    Icon VAR = PlatformIcons.VARIABLE_ICON;
    Icon VAL = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/value.png");
    Icon PARAMETER = PlatformIcons.PARAMETER_ICON;
    Icon FIELD_VAL = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/field_value.png");
    Icon FIELD_VAR = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/field_variable.png");
    Icon CLASS_INITIALIZER = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/classInitializerKotlin.png");
    Icon TYPE_ALIAS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/typeAlias.png");

    Icon SUSPEND_CALL = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/suspendCall.png");
    // TODO: change icon files themselves!
    Icon FROM_EXPECTED = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/fromHeader.png");
    Icon FROM_ACTUAL = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/fromImpl.png");

    Icon LAUNCH = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_launch_configuration.png");

    Icon JS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_js.png");
}
