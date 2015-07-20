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

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.projectRoots.Sdk
import org.jetbrains.kotlin.idea.configuration.ui.AbsentJdkAnnotationsComponent
import org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtil
import javax.swing.event.HyperlinkEvent

public class AbsentSdkAnnotationsNotification(sdks: Collection<Sdk>, title: String, val text: String) :
    Notification(
            AbsentJdkAnnotationsComponent.EXTERNAL_ANNOTATIONS_GROUP_ID,
            title, text,
            NotificationType.WARNING,
            AbsentAnnotationsListener(sdks) // Workaround for KT-4086
    )
{
    override fun equals(obj: Any?) = obj is AbsentSdkAnnotationsNotification && text == obj.text && getTitle() == obj.getTitle()

    override fun hashCode(): Int = 31 * getTitle().hashCode() + text.hashCode()
}

fun isAndroidSdk(sdk: Sdk) = sdk.getSdkType().getName() == "Android SDK"

fun getNotificationTitle(sdks: Collection<Sdk>) = "Kotlin external annotations for ${getSdkKind(sdks)} are not set for " + getSdkName(sdks)

fun getNotificationString(sdks: Collection<Sdk>) = "<a href=\"configure\">Set up Kotlin ${getSdkKind(sdks)} annotations</a>"

private class AbsentAnnotationsListener(val sdks: Collection<Sdk>): NotificationListener {
    override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (event.getDescription() == "configure") {
                for (sdk in sdks) {
                    if (isAndroidSdk(sdk)) {
                        if (!KotlinRuntimeLibraryUtil.androidSdkAnnotationsArePresent(sdk)) {
                            KotlinRuntimeLibraryUtil.addAndroidSdkAnnotations(sdk);
                        }
                        if (KotlinRuntimeLibraryUtil.jdkAnnotationsArePresent(sdk)) {
                            KotlinRuntimeLibraryUtil.removeJdkAnnotations(sdk);
                        }
                    }
                    else {
                        KotlinRuntimeLibraryUtil.addJdkAnnotations(sdk);
                    }
                }
            }
            notification.expire()
        }
    }
}

private fun getSdkKind(sdks: Collection<Sdk>) = if (sdks.size() > 1) "SDK" else if (isAndroidSdk(sdks.first())) "Android SDK" else "JDK"

private fun getSdkName(sdks: Collection<Sdk>) = if (sdks.size() > 1) "several SDKs" else "'${sdks.first().getName()}'"


