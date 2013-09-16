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

package org.jetbrains.jet.plugin.configuration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;

import javax.swing.event.HyperlinkEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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

    public static boolean hasKotlinFiles(@NotNull Module module) {
        return !FileTypeIndex.getFiles(JetFileType.INSTANCE, GlobalSearchScope.moduleScope(module)).isEmpty();
    }

    public static Collection<Module> getModulesWithKotlinFiles(@NotNull Project project) {
        List<Module> modulesWithKotlin = Lists.newArrayList();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (hasKotlinFiles(module)) {
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

    private static void showConfigureKotlinNotification(final Project project) {
        Notifications.Bus.notify(
                new Notification("Configure Kotlin",
                                 "Kotlin file(s) found in your project.",
                                 getNotificationString(project),
                                 NotificationType.WARNING, new NotificationListener() {
                    @Override
                    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            KotlinProjectConfigurator configurator = getConfiguratorByName(event.getDescription());
                            if (configurator == null) {
                                throw new AssertionError("Missed action: " + event.getDescription());
                            }
                            configurator.configure(project);
                            notification.expire();
                        }
                    }
                }), project);
    }

    private static String getNotificationString(Project project) {
        StringBuilder builder = new StringBuilder("Configure ");

        Collection<Module> modules = getModulesWithKotlinFiles(project);
        final boolean isOnlyOneModule = modules.size() == 1;
        if (isOnlyOneModule) {
            builder.append("'").append(modules.iterator().next().getName()).append("' module");
        }
        else {
            builder.append("modules");
        }

        builder.append(" in '").append(project.getName()).append("' project");
        builder.append("\n");

        String links = StringUtil.join(getApplicableConfigurators(project), new Function<KotlinProjectConfigurator, String>() {
            @Override
            public String fun(KotlinProjectConfigurator configurator) {
                return getLink(configurator, isOnlyOneModule);
            }
        }, "  ");
        builder.append(links);

        return builder.toString();
    }

    @NotNull
    public static Collection<KotlinProjectConfigurator> getApplicableConfigurators(@NotNull Project project) {
        Set<KotlinProjectConfigurator> applicableConfigurators = Sets.newHashSet();
        for (KotlinProjectConfigurator configurator : Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)) {
            for (Module module : getNonConfiguredModules(project, configurator)) {
                if (configurator.isApplicable(module)) {
                    applicableConfigurators.add(configurator);
                }
            }
        }
        return applicableConfigurators;
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
    public static String getLink(@NotNull KotlinProjectConfigurator configurator, boolean isOnlyOneModule) {
        return StringUtil.join("<a href=\"", configurator.getName(), "\">as Kotlin (",
                               configurator.getPresentableText(),
                               isOnlyOneModule ? ") module" : ") modules",
                               "</a>");
    }

    private ConfigureKotlinInProjectUtils() {
    }

    public static void showInfoNotification(@NotNull String message) {
        Notifications.Bus.notify(new Notification("Configure Kotlin", "Configure Kotlin", message, NotificationType.INFORMATION));
    }
}
