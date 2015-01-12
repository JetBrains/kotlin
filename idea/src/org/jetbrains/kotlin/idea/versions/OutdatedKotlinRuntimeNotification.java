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

package org.jetbrains.kotlin.idea.versions;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.framework.library.LibraryVersionProperties;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JetPluginUtil;
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider;
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider;
import org.jetbrains.kotlin.idea.framework.LibraryPresentationProviderUtil;

import javax.swing.event.HyperlinkEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.intellij.util.PathUtil.getLocalFile;

public class OutdatedKotlinRuntimeNotification extends AbstractProjectComponent {
    private static final String SUPPRESSED_PROPERTY_NAME = "oudtdated.runtime.suppressed.plugin.version";
    private static final String OUTDATED_RUNTIME_GROUP_DISPLAY_ID = "Outdated Kotlin Runtime";

    public OutdatedKotlinRuntimeNotification(Project project) {
        super(project);
    }

    private static class VersionedLibrary extends Pair<Library, String> {
        public VersionedLibrary(@NotNull Library library, @Nullable String version) {
            super(library, version);
        }

        @NotNull
        public Library getLibrary() {
            return first;
        }

        @Nullable
        public String getVersion() {
            return second;
        }
    }

    @Override
    public void projectOpened() {
        StartupManager.getInstance(myProject).registerPostStartupActivity(new Runnable() {
            @Override
            public void run() {
                final String pluginVersion = JetPluginUtil.getPluginVersion();
                if ("@snapshot@".equals(pluginVersion)) return; // plugin is run from sources, can't compare versions

                // user already clicked suppress
                if (pluginVersion.equals(PropertiesComponent.getInstance(myProject).getValue(SUPPRESSED_PROPERTY_NAME))) return;

                Collection<VersionedLibrary> versionedOutdatedLibraries = findOutdatedKotlinLibraries(myProject, pluginVersion);
                if (versionedOutdatedLibraries.isEmpty()) {
                    return;
                }

                Collection<Library> outdatedLibraries = extractLibraries(versionedOutdatedLibraries);

                String message;
                if (versionedOutdatedLibraries.size() == 1) {
                    VersionedLibrary versionedLibrary = versionedOutdatedLibraries.iterator().next();

                    String version = versionedLibrary.getVersion();
                    String readableVersion = version == null ? "unknown" : version;
                    String libraryName = versionedLibrary.getLibrary().getName();

                    message = String.format(
                            "<p>Your version of Kotlin runtime in '%s' library is %s, while plugin version is %s.</p>" +
                            "<p>Runtime library should be updated to avoid compatibility problems.</p>" +
                            "<p><a href=\"update\">Update Runtime</a> <a href=\"ignore\">Ignore</a></p>",
                            libraryName,
                            readableVersion,
                            pluginVersion);
                }
                else {
                    String libraryNames = StringUtil.join(outdatedLibraries, new Function<Library, String>() {
                        @Override
                        public String fun(Library library) {
                            return library.getName();
                        }
                    }, ", ");

                    message = String.format(
                            "<p>Version of Kotlin runtime is outdated in several libraries (%s). Plugin version is %s.</p>" +
                            "<p>Runtime libraries should be updated to avoid compatibility problems.</p>" +
                            "<p><a href=\"update\">Update All</a> <a href=\"ignore\">Ignore</a></p>",
                            libraryNames,
                            pluginVersion);
                }


                Notifications.Bus.notify(new Notification(OUTDATED_RUNTIME_GROUP_DISPLAY_ID, "Outdated Kotlin Runtime", message,
                                                          NotificationType.WARNING, new NotificationListener() {
                    @Override
                    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            if ("update".equals(event.getDescription())) {
                                Collection<VersionedLibrary> versionedOutdatedLibraries = findOutdatedKotlinLibraries(myProject, pluginVersion);
                                Collection<Library> outdatedLibraries = extractLibraries(versionedOutdatedLibraries);
                                KotlinRuntimeLibraryUtil.updateLibraries(myProject, outdatedLibraries);
                                suggestDeleteKotlinJsIfNeeded(outdatedLibraries);
                            }
                            else if ("ignore".equals(event.getDescription())) {
                                PropertiesComponent.getInstance(myProject).setValue(SUPPRESSED_PROPERTY_NAME, pluginVersion);
                            }
                            else {
                                throw new AssertionError();
                            }
                            notification.expire();
                        }
                    }
                }), myProject);
            }
        });
    }

    private void deleteKotlinJs() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        VirtualFile kotlinJsFile = myProject.getBaseDir().findFileByRelativePath("script/kotlin.js");
                        if (kotlinJsFile == null) return;

                        VirtualFile fileToDelete = getLocalFile(kotlinJsFile);
                        try {
                            VirtualFile parent = fileToDelete.getParent();
                            fileToDelete.delete(this);
                            parent.refresh(false, true);
                        }
                        catch (IOException ex) {
                            Notifications.Bus.notify(
                                new Notification(OUTDATED_RUNTIME_GROUP_DISPLAY_ID, "Error", "Could not delete 'script/kotlin.js': " + ex.getMessage(), NotificationType.ERROR));
                        }
                    }
                });
            }
        });
    }

    private void suggestDeleteKotlinJsIfNeeded(Collection<Library> outdatedLibraries) {
        VirtualFile kotlinJsFile = myProject.getBaseDir().findFileByRelativePath("script/kotlin.js");
        if (kotlinJsFile == null) return;

        boolean addNotification = false;
        for(Library library : outdatedLibraries) {
            if (LibraryPresentationProviderUtil.isDetected(JSLibraryStdPresentationProvider.getInstance(), library)) {
                VirtualFile jsStdlibJar = JSLibraryStdPresentationProvider.getJsStdLibJar(library);
                assert jsStdlibJar != null : "jslibFile should not be null";

                if (jsStdlibJar.findFileByRelativePath("kotlin.js") == null) {
                    addNotification = true;
                    break;
                }
            }
        }
        if (!addNotification) return;

        String message = String.format(
                "<p>File 'script/kotlin.js' was probably created by an older version of the Kotlin plugin.</p>" +
                "<p>The new Kotlin plugin copies an up-to-date version of this file to the output directory automatically, so the old version of it can be deleted.</p>" +
                "<p><a href=\"delete\">Delete this file</a> <a href=\"ignore\">Ignore</a></p>");

        Notifications.Bus.notify(new Notification(OUTDATED_RUNTIME_GROUP_DISPLAY_ID, "Outdated Kotlin Runtime", message,
                                                  NotificationType.WARNING, new NotificationListener() {
            @Override
            public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if ("delete".equals(event.getDescription())) {
                        deleteKotlinJs();
                    }
                    else if ("ignore".equals(event.getDescription())) {
                        // pass
                    }
                    else {
                        throw new AssertionError();
                    }
                    notification.expire();
                }
            }
        }), myProject);
    }

    private static Collection<Library> extractLibraries(Collection<VersionedLibrary> libraries) {
        return Collections2.transform(libraries, new com.google.common.base.Function<VersionedLibrary, Library>() {
            @Override
            public Library apply(@Nullable VersionedLibrary versionedLibrary) {
                assert versionedLibrary != null;
                return versionedLibrary.getLibrary();
            }
        });
    }

    @NotNull
    private static Collection<VersionedLibrary> findOutdatedKotlinLibraries(@NotNull Project project, @NotNull String pluginVersion) {
        List<VersionedLibrary> outdatedLibraries = Lists.newArrayList();

        for (Library library : KotlinRuntimeLibraryUtil.findKotlinLibraries(project)) {
            LibraryVersionProperties libraryVersionProperties =
                    LibraryPresentationProviderUtil.getLibraryProperties(JavaRuntimePresentationProvider.getInstance(), library);
            if (libraryVersionProperties == null) {
                libraryVersionProperties =
                        LibraryPresentationProviderUtil.getLibraryProperties(JSLibraryStdPresentationProvider.getInstance(), library);
            }
            if (libraryVersionProperties == null) {
                continue;
            }
            String libraryVersion = libraryVersionProperties.getVersionString();

            boolean isOutdated = "snapshot".equals(libraryVersion)
                                 || libraryVersion == null
                                 || libraryVersion.startsWith("internal-") != pluginVersion.startsWith("internal-")
                                 || VersionComparatorUtil.compare(pluginVersion, libraryVersion) > 0;

            if (isOutdated) {
                outdatedLibraries.add(new VersionedLibrary(library, libraryVersion));
            }
        }

        return outdatedLibraries;
    }


    @NotNull
    public static Runnable showRuntimeJarNotFoundDialog(@NotNull final Project project, final @NotNull String jarName) {
        return new Runnable() {
            @Override
            public void run() {
                Messages.showErrorDialog(project,
                                         jarName + " is not found. Make sure plugin is properly installed.",
                                         "No Runtime Found");
            }
        };
    }
}
