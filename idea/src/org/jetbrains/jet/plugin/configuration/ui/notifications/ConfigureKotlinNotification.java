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

package org.jetbrains.jet.plugin.configuration.ui.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.jet.plugin.configuration.KotlinProjectConfigurator;
import org.jetbrains.jet.plugin.configuration.ui.NonConfiguredKotlinProjectComponent;

import javax.swing.event.HyperlinkEvent;
import java.util.Collection;

import static kotlin.KotlinPackage.first;

public class ConfigureKotlinNotification extends Notification {
    private static final String TITLE = "Configure Kotlin";

    @NotNull private final String notificationText;

    public ConfigureKotlinNotification(
            @NotNull final Project project,
            @NotNull String notificationText
    ) {
        super(NonConfiguredKotlinProjectComponent.CONFIGURE_NOTIFICATION_GROUP_ID, TITLE, notificationText, NotificationType.WARNING, new NotificationListener() {
            @Override
            public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    KotlinProjectConfigurator configurator = ConfigureKotlinInProjectUtils.getConfiguratorByName(event.getDescription());
                    if (configurator == null) {
                        throw new AssertionError("Missed action: " + event.getDescription());
                    }
                    configurator.configure(project);
                    notification.expire();
                }
            }
        });

        this.notificationText = notificationText;
    }

    @NotNull
    public static String getNotificationString(Project project) {
        Collection<Module> modules = ConfigureKotlinInProjectUtils.getNonConfiguredModules(project);

        final boolean isOnlyOneModule = modules.size() == 1;

        String modulesString = isOnlyOneModule ? String.format("'%s' module", first(modules).getName()) : "modules";
        String links = StringUtil.join(ConfigureKotlinInProjectUtils.getAbleToRunConfigurators(project), new Function<KotlinProjectConfigurator, String>() {
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

        if (!notificationText.equals(that.notificationText)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return notificationText.hashCode();
    }
}
