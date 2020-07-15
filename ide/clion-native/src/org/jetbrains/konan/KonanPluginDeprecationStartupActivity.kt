package org.jetbrains.konan

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import java.util.concurrent.TimeUnit

class KonanPluginDeprecationStartupActivity : StartupActivity {
    companion object {
        private const val TITLE = "Kotlin/Native for CLion plugin deprecation"
        private val CONTENT = """
            <html>
            We've launched a deprecation cycle for the Kotlin/Native for CLion IDE plugin.<br/><br/>
            Originally it was intended for debugging of Kotlin/Native executables. Nowadays this capability is available in <a href="https://plugins.jetbrains.com/plugin/12775-native-debugging-support/">IJ IDEA Ultimate</a>.<br/><br/>
            We will stop publishing Kotlin/Native for CLion IDE plugin after 1.4.0 release. Please <a href="${ApplicationInfoEx.getInstanceEx().supportUrl}">contact</a> us if this deprecation causes any problem, we will help you to solve it.
            </html>
        """.trimIndent()

        private val NOTIFICATION_GROUP = NotificationGroup(
                displayId = "Kotlin/Native for CLion Plugin Deprecation",
                displayType = NotificationDisplayType.STICKY_BALLOON,
                isLogByDefault = true
        )

        private const val PROPERTY_DONT_SHOW = "kotlin.native.clion.deprecation.dontShow"
        private const val PROPERTY_SHOWN_LAST_TIME = "kotlin.native.clion.deprecation.shownLastTime"

        private val MIN_NOTIFICATION_INTERVAL = TimeUnit.DAYS.toMillis(1)
    }

    override fun runActivity(project: Project) {
        if (isUnitTestMode()) return

        val properties = PropertiesComponent.getInstance()
        if (properties.getBoolean(PROPERTY_DONT_SHOW)) return // don't display it if it was suppressed intentionally

        val shownLastTime = properties.getLong(PROPERTY_SHOWN_LAST_TIME, 0)
        val currentTime = System.currentTimeMillis()

        if (shownLastTime + MIN_NOTIFICATION_INTERVAL > currentTime) return // don't bother again in the nearest 24h
        properties.setValue(PROPERTY_SHOWN_LAST_TIME, currentTime.toString())

        val notification = NOTIFICATION_GROUP.createNotification(
                TITLE,
                CONTENT,
                NotificationType.WARNING,
                NotificationListener.UrlOpeningListener(false)
        )
        notification.addAction(object : AnAction("Don't show again") {
            override fun actionPerformed(event: AnActionEvent) {
                properties.setValue(PROPERTY_DONT_SHOW, true)
                notification.expire()
            }
        })

        Notifications.Bus.notify(notification, project)
    }
}
