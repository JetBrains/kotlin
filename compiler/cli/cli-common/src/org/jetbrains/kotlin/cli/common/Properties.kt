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

package org.jetbrains.kotlin.cli.common

import com.intellij.util.LineSeparator
import java.util.*

enum class CompilerSystemProperties(val property: String) {
    COMPILE_DAEMON_ENABLED_PROPERTY("kotlin.daemon.enabled"),
    COMPILE_DAEMON_JVM_OPTIONS_PROPERTY("kotlin.daemon.jvm.options"),
    COMPILE_DAEMON_OPTIONS_PROPERTY("kotlin.daemon.options"),
    COMPILE_DAEMON_CLIENT_OPTIONS_PROPERTY("kotlin.daemon.client.options"),
    COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY("kotlin.daemon.client.alive.path"),
    COMPILE_DAEMON_LOG_PATH_PROPERTY("kotlin.daemon.log.path"),
    COMPILE_DAEMON_REPORT_PERF_PROPERTY("kotlin.daemon.perf"),
    COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY("kotlin.daemon.verbose"),
    COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY("kotlin.daemon.startup.timeout"),
    JAVA_RMI_SERVER_HOSTNAME("java.rmi.server.hostname"),
    DAEMON_RMI_SOCKET_BACKLOG_SIZE_PROPERTY("kotlin.daemon.socket.backlog.size"),
    DAEMON_RMI_SOCKET_CONNECT_ATTEMPTS_PROPERTY("kotlin.daemon.socket.connect.attempts"),
    DAEMON_RMI_SOCKET_CONNECT_INTERVAL_PROPERTY("kotlin.daemon.socket.connect.interval"),
    KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY("kotlin.environment.keepalive"),
    COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS("kotlin.daemon.custom.run.files.path.for.tests"),
    KOTLIN_COLORS_ENABLED_PROPERTY("kotlin.colors.enabled"),
    OS_NAME("os.name"),
    TMP_DIR("java.io.tmpdir"),
    USER_HOME("user.home"),
    JAVA_VERSION("java.specification.version"),
    JAVA_HOME("java.home"),
    JAVA_CLASS_PATH("java.class.path"),
    ;

    var value
        get() = (systemPropertyGetter ?: System::getProperty)(property)
        set(value) {
            (systemPropertySetter ?: System::setProperty)(property, value!!)
        }

    val safeValue
        get() = value ?: error("No value for $property system property")

    fun clear(): String? = (systemPropertyCleaner ?: System::clearProperty)(property)

    companion object {
        var systemPropertyGetter: ((String) -> String?)? = null

        var systemPropertySetter: ((String, String) -> String?)? = null

        var systemPropertyCleaner: ((String) -> String?)? = null
    }
}

val isWindows: Boolean
    get() = CompilerSystemProperties.OS_NAME.value!!.toLowerCase(Locale.ENGLISH).startsWith("windows")

fun String?.toBooleanLenient(): Boolean? = when (this?.toLowerCase()) {
    null -> false
    in listOf("", "yes", "true", "on", "y") -> true
    in listOf("no", "false", "off", "n") -> false
    else -> null
}
