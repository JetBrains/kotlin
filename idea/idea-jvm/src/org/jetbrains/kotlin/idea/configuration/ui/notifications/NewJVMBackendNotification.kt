/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.util.runReadActionInSmartMode


private const val NEW_JVM_BACKEND_NOTIFICATION_ID = "New JVM Backend"
private const val NEW_JVM_BACKEND_WAS_PROMOTED = "new.jvm.backend.notification"
private const val NEW_JVM_BACKEND_BLOG_POST = "https://blog.jetbrains.com/kotlin/2021/02/the-jvm-backend-is-in-beta-let-s-make-it-stable-together/"

fun notifyNewJVMBackendIfNeeded(project: Project) {
    val propertiesComponent = PropertiesComponent.getInstance()
    if (propertiesComponent.getBoolean(NEW_JVM_BACKEND_WAS_PROMOTED)) return

    val kotlinVersion = KotlinVersion.CURRENT
    if (kotlinVersion < KotlinVersion(1, 4, 30) || kotlinVersion >= KotlinVersion(1, 5)) return

    val hasKotlinFile = project.runReadActionInSmartMode {
        FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, GlobalSearchScope.projectScope(project))
    }

    if (!hasKotlinFile) return

    val notification = Notification(
        NEW_JVM_BACKEND_NOTIFICATION_ID,
        KotlinBundle.message("notification.title.new.jvm.ir.backend"),
        KotlinBundle.message("notification.message.new.jvm.ir.backend"),
        NotificationType.INFORMATION,
    ).apply {
        addAction(BrowseNotificationAction(KotlinBundle.message("notification.action.new.jvm.ir.backend"), NEW_JVM_BACKEND_BLOG_POST))

        isImportant = true
    }

    NotificationsConfiguration.getNotificationsConfiguration().register(
        NEW_JVM_BACKEND_NOTIFICATION_ID,
        NotificationDisplayType.STICKY_BALLOON,
    )

    notification.notify(project)
    propertiesComponent.setValue(NEW_JVM_BACKEND_WAS_PROMOTED, true)
}

