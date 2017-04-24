/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android;


import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import org.jetbrains.android.sdk.AndroidSdkAdditionalData;
import org.jetbrains.android.sdk.AndroidSdkData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for com.android.tools.idea.sdk.AndroidSdks
 * stabbed to minimize changes in AndroidTestBase
 */
public class AndroidSdks {

    private static AndroidSdks INSTANCE;

    private AndroidSdkData mySdkData;

    private AndroidSdks() {

    }

    public static AndroidSdks getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AndroidSdks();
        }

        return INSTANCE;
    }

    public AndroidSdkData tryToChooseAndroidSdk() {
        return mySdkData;
    }

    public void setSdkData(AndroidSdkData data) {
        mySdkData = data;
    }

    @Nullable
    public AndroidSdkAdditionalData getAndroidSdkAdditionalData(@NotNull Sdk sdk) {
        SdkAdditionalData data = sdk.getSdkAdditionalData();
        return data instanceof AndroidSdkAdditionalData ? (AndroidSdkAdditionalData)data : null;
    }
}
