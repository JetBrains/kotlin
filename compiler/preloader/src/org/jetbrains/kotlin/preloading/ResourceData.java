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

package org.jetbrains.kotlin.preloading;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public final class ResourceData {
    public final File jarFile;
    public final String resourceName;
    public final byte[] bytes;

    public ResourceData(File jarFile, String resourceName, byte[] bytes) {
        this.jarFile = jarFile;
        this.resourceName = resourceName;
        this.bytes = bytes;
    }

    public URL getURL() {
        try {
            String path = "file:" + jarFile + "!/" + resourceName;
            return new URL("jar", null, 0, path, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) throws IOException {
                    return new URLConnection(u) {
                        @Override
                        public void connect() throws IOException {}

                        @Override
                        public InputStream getInputStream() throws IOException {
                            return new ByteArrayInputStream(bytes);
                        }
                    };
                }
            });
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
