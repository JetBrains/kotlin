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

package org.jetbrains.kotlin.idea.framework.ui;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.text.VersionComparatorUtil;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtilsKt;
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator;
import org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtilKt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConfigureDialogWithModulesAndVersion extends DialogWrapper {
    private static final String VERSIONS_LIST_URL =
            "http://search.maven.org/solrsearch/select?q=g:%22org.jetbrains.kotlin%22+AND+a:%22kotlin-runtime%22&core=gav&rows=20&wt=json";

    private static final String EAP_VERSIONS_URL =
            "https://bintray.com/kotlin/kotlin-eap/kotlin/";

    private final ChooseModulePanel chooseModulePanel;

    private JPanel contentPane;
    private JPanel chooseModulesPanelPlace;
    private JComboBox kotlinVersionComboBox;
    private JPanel infoPanel;

    private final AsyncProcessIcon processIcon = new AsyncProcessIcon("loader");

    public ConfigureDialogWithModulesAndVersion(
            @NotNull Project project,
            @NotNull KotlinProjectConfigurator configurator,
            @NotNull Collection<Module> excludeModules
    ) {
        super(project);

        setTitle("Configure Kotlin in Project");

        init();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Find Kotlin Maven plugin versions", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                loadKotlinVersions();
            }
        });

        kotlinVersionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                updateComponents();
            }
        });

        kotlinVersionComboBox.addItem("loading...");
        kotlinVersionComboBox.setEnabled(false);

        processIcon.resume();
        infoPanel.add(processIcon, BorderLayout.CENTER);

        chooseModulePanel = new ChooseModulePanel(project, configurator, excludeModules);
        chooseModulesPanelPlace.add(chooseModulePanel.getContentPane(), BorderLayout.CENTER);

        updateComponents();
    }

    public List<Module> getModulesToConfigure() {
        return chooseModulePanel.getModulesToConfigure();
    }

    public String getKotlinVersion() {
        return (String) kotlinVersionComboBox.getSelectedItem();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void loadKotlinVersions() {
        Collection<String> items;
        try {
            items = loadVersions();
            hideLoader();
        }
        catch (Throwable t) {
            items = Collections.singletonList("1.0.0");
            showWarning();
        }
        updateVersions(items);
    }

    private void hideLoader() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                infoPanel.setVisible(false);
                infoPanel.updateUI();
            }
        }, ModalityState.stateForComponent(infoPanel));
    }

    private void showWarning() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                infoPanel.remove(processIcon);
                infoPanel.add(new JLabel(UIUtil.getBalloonWarningIcon()), BorderLayout.CENTER);
                infoPanel.setToolTipText("Couldn't load versions list from search.maven.org");
                infoPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                infoPanel.updateUI();
            }
        }, ModalityState.stateForComponent(infoPanel));
    }

    private void updateVersions(@NotNull final Collection<String> newItems) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                kotlinVersionComboBox.removeAllItems();
                kotlinVersionComboBox.setEnabled(true);
                for (String newItem : newItems) {
                    kotlinVersionComboBox.addItem(newItem);
                }
                kotlinVersionComboBox.setSelectedIndex(0);
            }
        }, ModalityState.stateForComponent(kotlinVersionComboBox));
    }

    @NotNull
    protected static Collection<String> loadVersions() throws Exception {
        List<String> versions = Lists.newArrayList();

        String bundledRuntimeVersion = KotlinRuntimeLibraryUtilKt.bundledRuntimeVersion();
        if (ConfigureKotlinInProjectUtilsKt.isEap(bundledRuntimeVersion)) {
            HttpURLConnection eapConnection = HttpConfigurable.getInstance().openHttpConnection(EAP_VERSIONS_URL + bundledRuntimeVersion);
            try {
                int timeout = (int) TimeUnit.SECONDS.toMillis(30);
                eapConnection.setConnectTimeout(timeout);
                eapConnection.setReadTimeout(timeout);

                if (eapConnection.getResponseCode() == 200) {
                    versions.add(bundledRuntimeVersion);
                }
            }
            finally {
                eapConnection.disconnect();
            }
        }

        HttpURLConnection urlConnection = HttpConfigurable.getInstance().openHttpConnection(VERSIONS_LIST_URL);
        try {
            int timeout = (int) TimeUnit.SECONDS.toMillis(30);
            urlConnection.setConnectTimeout(timeout);
            urlConnection.setReadTimeout(timeout);

            urlConnection.connect();

            InputStreamReader streamReader = new InputStreamReader(urlConnection.getInputStream());
            try {
                JsonElement rootElement = new JsonParser().parse(streamReader);
                JsonArray docsElements = rootElement.getAsJsonObject().get("response").getAsJsonObject().get("docs").getAsJsonArray();

                for (JsonElement element : docsElements) {
                    String versionNumber = element.getAsJsonObject().get("v").getAsString();
                    if (VersionComparatorUtil.compare("1.0.0", versionNumber) <= 0) {
                        versions.add(versionNumber);
                    }
                }
            }
            finally {
                Closeables.closeQuietly(streamReader);
            }
        }
        finally {
            urlConnection.disconnect();
        }

        return versions;
    }

    private void updateComponents() {
        setOKActionEnabled(kotlinVersionComboBox.isEnabled());
    }
}
