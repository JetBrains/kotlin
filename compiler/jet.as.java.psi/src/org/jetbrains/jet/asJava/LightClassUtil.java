/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.asJava;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.utils.KotlinVfsUtil;

import java.net.MalformedURLException;
import java.net.URL;

public class LightClassUtil {
    private static final Logger LOG = Logger.getInstance(LightClassUtil.class);
    private static final String DEFINITION_OF_ANY = "Any.jet";

    /**
     * Checks whether the given file is loaded from the location where Kotlin's built-in classes are defined.
     * As of today, this is compiler/frontend/src/jet directory and files such as Any.jet, Nothing.jet etc.
     *
     * Used to skip JetLightClass creation for built-ins, because built-in classes have no Java counterparts
     */
    public static boolean belongsToKotlinBuiltIns(@NotNull JetFile file) {
        try {
            String jetVfsPathUrl = KotlinVfsUtil.convertFromUrl(getBuiltInsDirResourceUrl());
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                VirtualFile parent = virtualFile.getParent();
                if (parent != null) {
                    String fileDirVfsUrl = parent.getUrl() + "/" + DEFINITION_OF_ANY;
                    if (jetVfsPathUrl.equals(fileDirVfsUrl)) {
                        return true;
                    }
                }
            }
        }
        catch (MalformedURLException e) {
            LOG.error(e);
        }
        // We deliberately return false on error: who knows what weird URLs we might come across out there
        // it would be a pity if no light classes would be created in such cases
        return false;
    }

    @NotNull
    public static URL getBuiltInsDirResourceUrl() {
        String pathToAny = "/" + KotlinBuiltIns.BUILT_INS_DIR + "/" + DEFINITION_OF_ANY;
        URL url = KotlinBuiltIns.class.getResource(pathToAny);
        if (url == null) {
            throw new IllegalStateException("Built-ins not found in the classpath: " + pathToAny);
        }
        return url;
    }

    /*package*/ static void logErrorWithOSInfo(@Nullable Throwable cause, @NotNull FqName fqName, @Nullable VirtualFile virtualFile) {
        String path = virtualFile == null ? "<null>" : virtualFile.getPath();
        LOG.error(
                "Could not generate LightClass for " + fqName + " declared in " + path + "\n" +
                "built-ins dir URL is " + getBuiltInsDirResourceUrl() + "\n" +
                "System: " + SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION + " Java Runtime: " + SystemInfo.JAVA_RUNTIME_VERSION,
                cause);
    }

    private LightClassUtil() {}
}
