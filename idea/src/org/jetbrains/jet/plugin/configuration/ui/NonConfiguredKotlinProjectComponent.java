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

package org.jetbrains.jet.plugin.configuration.ui;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils;

public class NonConfiguredKotlinProjectComponent extends AbstractProjectComponent {
    public static final String CONFIGURE_NOTIFICATION_GROUP_ID = "Configure Kotlin in Project";

    protected NonConfiguredKotlinProjectComponent(Project project) {
        super(project);

        NotificationsConfiguration.getNotificationsConfiguration().
                register(CONFIGURE_NOTIFICATION_GROUP_ID, NotificationDisplayType.STICKY_BALLOON, true);
    }

    @Override
    public void projectOpened() {
        super.projectOpened();

        StartupManager.getInstance(myProject).registerPostStartupActivity(new Runnable() {
            @Override
            public void run() {
                ConfigureKotlinInProjectUtils.showConfigureKotlinNotificationIfNeeded(myProject);
            }
        });
    }
}
