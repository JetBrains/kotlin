/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Base64;

public class BytesUrlUtils {

    private static final URLStreamHandler BYTES_URL_HANDLER = new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL url) {
            return new URLConnection(url) {
                @Override
                public void connect() {
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(Base64.getDecoder().decode(url.getPath()));
                }
            };
        }
    };

    /**
     * Encode the entire [bytes] array in the self-contained URL with "bytes" protocol
     * @param bytes byte array to encode into the URL
     * @return the URL containing encoded [bytes] contents
     * @throws MalformedURLException
     */
    @Nullable
    public static URL createBytesUrl(@NotNull byte[] bytes) throws MalformedURLException {
        return new URL(null, "bytes:" + Base64.getEncoder().encodeToString(bytes), BYTES_URL_HANDLER);
    }
}
