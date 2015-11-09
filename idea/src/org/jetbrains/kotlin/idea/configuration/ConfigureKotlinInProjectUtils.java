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

package org.jetbrains.kotlin.idea.configuration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinFileType;

import java.util.*;

public class ConfigureKotlinInProjectUtils {
    public static boolean isProjectConfigured(@NotNull Project project) {
        Collection<Module> modules = getModulesWithKotlinFiles(project);
        for (Module module : modules) {
            if (!isModuleConfigured(module)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isModuleConfigured(@NotNull Module module) {
        Set<KotlinProjectConfigurator> configurators = getApplicableConfigurators(module);
        for (KotlinProjectConfigurator configurator : configurators) {
            if (configurator.isConfigured(module)) {
                return true;
            }
        }
        return false;
    }

    public static Collection<Module> getModulesWithKotlinFiles(@NotNull Project project) {
        if (project.isDisposed()) {
            return Collections.emptyList();
        }

        if (!FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, GlobalSearchScope.projectScope(project))) {
            return Collections.emptyList();
        }

        List<Module> modulesWithKotlin = Lists.newArrayList();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, module.getModuleScope(true))) {
                modulesWithKotlin.add(module);
            }
        }

        return modulesWithKotlin;
    }

    public static void showConfigureKotlinNotificationIfNeeded(@NotNull Module module) {
        if (isModuleConfigured(module)) return;

        showConfigureKotlinNotification(module.getProject());
    }

    public static void showConfigureKotlinNotificationIfNeeded(@NotNull Project project) {
        if (isProjectConfigured(project)) return;

        showConfigureKotlinNotification(project);
    }

    private static void showConfigureKotlinNotification(@NotNull Project project) {
        ConfigureKotlinNotificationManager.INSTANCE$.notify(project);
    }

    @NotNull
    public static Collection<KotlinProjectConfigurator> getAbleToRunConfigurators(@NotNull Project project) {
        Collection<Module> modules = getModulesWithKotlinFiles(project);

        Set<KotlinProjectConfigurator> canRunConfigurators = Sets.newLinkedHashSet();
        for (KotlinProjectConfigurator configurator : Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)) {
            for (Module module : modules) {
                if (configurator.isApplicable(module) && !configurator.isConfigured(module)) {
                    canRunConfigurators.add(configurator);
                    break;
                }
            }
        }

        return canRunConfigurators;
    }

    @NotNull
    public static Set<KotlinProjectConfigurator> getApplicableConfigurators(@NotNull Module module) {
        Set<KotlinProjectConfigurator> applicableConfigurators = Sets.newHashSet();
        for (KotlinProjectConfigurator configurator : Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)) {
            if (configurator.isApplicable(module)) {
                applicableConfigurators.add(configurator);
            }
        }
        return applicableConfigurators;
    }

    @Nullable
    public static KotlinProjectConfigurator getConfiguratorByName(@NotNull String name) {
        for (KotlinProjectConfigurator configurator : Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)) {
            if (configurator.getName().equals(name)) {
                return configurator;
            }
        }
        return null;
    }

    public static List<Module> getNonConfiguredModules(@NotNull Project project, @NotNull KotlinProjectConfigurator configurator) {
        Collection<Module> modules = getModulesWithKotlinFiles(project);
        List<Module> result = new ArrayList<Module>(modules.size());
        for (Module module : modules) {
            if (configurator.isApplicable(module) && !configurator.isConfigured(module)) {
                result.add(module);
            }
        }
        return result;
    }

    @NotNull
    public static Collection<Module> getNonConfiguredModules(@NotNull Project project) {
        Set<Module> modules = Sets.newHashSet();
        Collection<Module> modulesWithKotlinFiles = getModulesWithKotlinFiles(project);

        for (KotlinProjectConfigurator configurator : getAbleToRunConfigurators(project)) {
            for (Module module : modulesWithKotlinFiles) {
                if (!configurator.isConfigured(module)) {
                    modules.add(module);
                }
            }
        }

        return modules;
    }

    private ConfigureKotlinInProjectUtils() {
    }

    public static void showInfoNotification(@NotNull Project project, @NotNull String message) {
        Notifications.Bus.notify(new Notification("Configure Kotlin: info notification", "Configure Kotlin", message, NotificationType.INFORMATION), project);
    }
}
