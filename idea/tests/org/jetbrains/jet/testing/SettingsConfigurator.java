/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.testing;

import org.jetbrains.jet.InTextDirectivesUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class SettingsConfigurator {
    private final String[] settingsToTrue;
    private final String[] settingsToFalse;
    private final Object[] objects;

    public SettingsConfigurator(String fileText, Object... objects) {
        settingsToTrue = InTextDirectivesUtils.findArrayWithPrefixes(fileText, "// SET_TRUE:");
        settingsToFalse = InTextDirectivesUtils.findArrayWithPrefixes(fileText, "// SET_FALSE:");
        this.objects = objects;
    }

    public static void setBooleanSetting(String settingName, boolean value, Object... objects) {
        for (Object object : objects) {
            if (setSettingWithField(settingName, object, value) || setSettingWithMethod(settingName, object, value)) {
                return;
            }
        }

        throw new IllegalArgumentException(String.format(
                "There's no property or method with name '%s' in given objects: %s", settingName, Arrays.toString(objects)));
    }

    public void configureSettings() {
        for (String trueSetting : settingsToTrue) {
            setBooleanSetting(trueSetting, true, objects);
        }

        for (String falseSetting : settingsToFalse) {
            setBooleanSetting(falseSetting, false, objects);
        }
    }

    public void configureInvertedSettings() {
        for (String trueSetting : settingsToTrue) {
            setBooleanSetting(trueSetting, false, objects);
        }

        for (String falseSetting : settingsToFalse) {
            setBooleanSetting(falseSetting, true, objects);
        }
    }

    private static boolean setSettingWithField(String settingName, Object object, boolean value) {
        try {
            Field field = object.getClass().getDeclaredField(settingName);
            field.setBoolean(object, value);
            return true;
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Can't set property with the name %s in object %s", settingName, object));
        }
        catch (NoSuchFieldException e) {
            // Do nothing - will try other variants
        }

        return false;
    }

    private static boolean setSettingWithMethod(String setterName, Object object, boolean value) {
        try {
            Method method = object.getClass().getMethod(setterName, boolean.class);
            method.invoke(object, value);
            return true;
        }
        catch (InvocationTargetException e) {
            throw new IllegalArgumentException(String.format("Can't call method with name %s for object %s", setterName, object));
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Can't access to method with name %s for object %s", setterName, object));
        }
        catch (NoSuchMethodException e) {
            // Do nothing - will try other variants
        }

        return false;
    }
}
