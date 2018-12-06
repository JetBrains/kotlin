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

package org.jetbrains.kotlin.idea.editor;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

@State(
        name = "JetEditorOptions",
        storages = {
                @Storage(
                        file = "$APP_CONFIG$/editor.xml"
                )}
)
public class KotlinEditorOptions implements PersistentStateComponent<KotlinEditorOptions> {
    private boolean donTShowConversionDialog = false;
    private boolean enableJavaToKotlinConversion = true;

    public boolean isDonTShowConversionDialog() {
        return donTShowConversionDialog;
    }

    public void setDonTShowConversionDialog(boolean donTShowConversionDialog) {
        this.donTShowConversionDialog = donTShowConversionDialog;
    }

    public boolean isEnableJavaToKotlinConversion() {
        return enableJavaToKotlinConversion;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setEnableJavaToKotlinConversion(boolean enableJavaToKotlinConversion) {
        this.enableJavaToKotlinConversion = enableJavaToKotlinConversion;
    }

    @Override
    public KotlinEditorOptions getState() {
        return this;
    }

    @Override
    public void loadState(KotlinEditorOptions state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static KotlinEditorOptions getInstance() {
        return ServiceManager.getService(KotlinEditorOptions.class);
    }

    @Override
    @Nullable
    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
