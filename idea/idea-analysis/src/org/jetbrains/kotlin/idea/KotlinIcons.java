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
    Icon SMALL_LOGO = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin." + IconExtensionChooser.iconExtension());

    Icon SMALL_LOGO_13 = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin13." + IconExtensionChooser.iconExtension());

    Icon CLASS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/classKotlin." + IconExtensionChooser.iconExtension());
    Icon ABSTRACT_CLASS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/abstractClassKotlin." + IconExtensionChooser.iconExtension());
    Icon ENUM = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/enumKotlin." + IconExtensionChooser.iconExtension());
    Icon FILE = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_file." + IconExtensionChooser.iconExtension());
    Icon SCRIPT = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_script." + IconExtensionChooser.iconExtension());
    Icon GRADLE_SCRIPT = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_gradle_script." + IconExtensionChooser.iconExtension());
    Icon OBJECT = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/objectKotlin." + IconExtensionChooser.iconExtension());
    Icon INTERFACE = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/interfaceKotlin." + IconExtensionChooser.iconExtension());
    Icon ANNOTATION = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/annotationKotlin." + IconExtensionChooser.iconExtension());
    // BUNCH: 182
    //todo: Use AllIcons.nodes instead (actually the same icon)
    Icon FUNCTION = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/function." + IconExtensionChooser.iconExtension());
    Icon EXTENSION_FUNCTION = PlatformIcons.FUNCTION_ICON;
    Icon ABSTRACT_EXTENSION_FUNCTION = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/abstract_extension_function." + IconExtensionChooser.iconExtension());
    Icon LAMBDA = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/lambda." + IconExtensionChooser.iconExtension());
    Icon VAR = PlatformIcons.VARIABLE_ICON;
    Icon VAL = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/value." + IconExtensionChooser.iconExtension());
    Icon PARAMETER = PlatformIcons.PARAMETER_ICON;
    Icon FIELD_VAL = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/field_value." + IconExtensionChooser.iconExtension());
    Icon FIELD_VAR = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/field_variable." + IconExtensionChooser.iconExtension());
    Icon CLASS_INITIALIZER = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/classInitializerKotlin." + IconExtensionChooser.iconExtension());
    Icon TYPE_ALIAS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/typeAlias." + IconExtensionChooser.iconExtension());

    Icon SUSPEND_CALL = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/suspendCall." + IconExtensionChooser.iconExtension());

    Icon ACTUAL = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/actual." + IconExtensionChooser.iconExtension());
    Icon EXPECT = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/expect." + IconExtensionChooser.iconExtension());

    Icon LAUNCH = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_launch_configuration." + IconExtensionChooser.iconExtension());

    Icon JS = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_js." + IconExtensionChooser.iconExtension());
    Icon MPP = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_multiplatform_project." + IconExtensionChooser.iconExtension());
    Icon NATIVE = IconLoader.getIcon("/org/jetbrains/kotlin/idea/icons/kotlin_native.svg");
}
