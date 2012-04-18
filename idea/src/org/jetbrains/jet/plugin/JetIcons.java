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

package org.jetbrains.jet.plugin;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * @author Nikolay Krasko
 */
public interface JetIcons {
    Icon SMALL_LOGO = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/kotlin16x16.png");

    Icon FILE = SMALL_LOGO; // TODO: Add file icon
    Icon OBJECT = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/object.png");
    Icon TRAIT = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/trait.png");
    Icon FUNCTION = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/function.png");
    Icon LAMBDA = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/lambda.png");
    Icon VAR = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/variable.png");
    Icon VAL = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/value.png");
    Icon PARAMETER = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/parameter.png");
    Icon FIELD_VAL = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/field_value.png");
    Icon FIELD_VAR = IconLoader.getIcon("/org/jetbrains/jet/plugin/icons/field_variable.png");
}
