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

package org.jetbrains.kotlin.idea.configuration.ui.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtilsKt;
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator;
import org.jetbrains.kotlin.idea.configuration.ui.KotlinConfigurationCheckerComponent;

import javax.swing.event.HyperlinkEvent;
import java.util.Collection;
import java.util.List;

import static kotlin.collections.CollectionsKt.first;

public class ConfigureKotlinNotification extends Notification {
    private static final String TITLE = "Configure Kotlin";

    public ConfigureKotlinNotification(
            @NotNull final Project project,
            @NotNull final List<Module> excludeModules
    ) {
        super(KotlinConfigurationCheckerComponent.CONFIGURE_NOTIFICATION_GROUP_ID, TITLE, getNotificationString(project, excludeModules),
              NotificationType.WARNING, new NotificationListener() {
            @Override
            public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    KotlinProjectConfigurator configurator = ConfigureKotlinInProjectUtilsKt.getConfiguratorByName(event.getDescription());
                    if (configurator == null) {
                        throw new AssertionError("Missed action: " + event.getDescription());
                    }
                    notification.expire();

                    configurator.configure(project, excludeModules);
                }
            }
        });
    }

    @NotNull
    public static String getNotificationString(Project project, Collection<Module> excludeModules) {
        Collection<Module> modules = ConfigureKotlinInProjectUtilsKt.getNonConfiguredModules(project, excludeModules);

        final boolean isOnlyOneModule = modules.size() == 1;

        String modulesString = isOnlyOneModule ? String.format("'%s' module", first(modules).getName()) : "modules";
        String links = StringUtil.join(ConfigureKotlinInProjectUtilsKt.getAbleToRunConfigurators(project), new Function<KotlinProjectConfigurator, String>() {
            @Override
            public String fun(KotlinProjectConfigurator configurator) {
                return getLink(configurator, isOnlyOneModule);
            }
        }, "<br/>");

        return String.format("Configure %s in '%s' project<br/> %s", modulesString, project.getName(), links);
    }

    @NotNull
    private static String getLink(@NotNull KotlinProjectConfigurator configurator, boolean isOnlyOneModule) {
        return StringUtil.join("<a href=\"", configurator.getName(), "\">as Kotlin (",
                               configurator.getPresentableText(),
                               isOnlyOneModule ? ") module" : ") modules",
                               "</a>");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigureKotlinNotification)) return false;

        ConfigureKotlinNotification that = (ConfigureKotlinNotification) o;

        if (!getContent().equals(that.getContent())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getContent().hashCode();
    }
}
