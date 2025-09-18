/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom SecurityManager implementation for Kotlin compiler.
 * This replaces the standard java.lang.SecurityManager.
 */
public class KotlinSecurityManager extends SecurityManager {
    private final Map<String, Boolean> readPermissionCache =
            Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 10_000;
                }
            });
    private final Map<String, Boolean> deletePermissionCache =
            Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 10_000;
                }
            });
    private final Map<String, Boolean> writePermissionCache =
            Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 10_000;
                }
            });
    private final Map<String, Boolean> execPermissionCache =
            Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 10_000;
                }
            });

    /**
     * Public constructor required for JVM instantiation via -Djava.security.manager flag.
     * This constructor is used by the JVM when creating the security manager.
     */
    public KotlinSecurityManager() {
        // Initialize security manager
        super();
    }

    /**
     * Check if the current execution context has permission to read the specified file.
     * This implementation uses a cache to avoid repeated permission checks for the same file.
     *
     * @param file the file to check read permission for
     * @exception SecurityException if the calling thread does not have
     *             permission to access the specified file.
     * @exception  NullPointerException if the <code>file</code> argument is
     *             <code>null</code>.
     * @see        #checkPermission(java.security.Permission) checkPermission
     */
    @Override
    public void checkRead(String file) {
        // Check if we've already verified this file
        if (readPermissionCache.containsKey(file)) {
            // Permission was granted, so we can return immediately
            return;
        }

        // Delegate to parent implementation for uncached files
        super.checkRead(file);
        // If we get here, permission was granted, so cache the result
        readPermissionCache.put(file, Boolean.TRUE);
    }

    @Override
    public void checkWrite(String file) {
        // Check if we've already verified this file
        if (writePermissionCache.containsKey(file)) {
            // Permission was granted, so we can return immediately
            return;
        }

        // Delegate to parent implementation for uncached files
        super.checkWrite(file);
        // If we get here, permission was granted, so cache the result
        writePermissionCache.put(file, Boolean.TRUE);
    }

    @Override
    public void checkDelete(String file) {
        // Check if we've already verified this file
        if (deletePermissionCache.containsKey(file)) {
            // Permission was granted, so we can return immediately
            return;
        }

        // Delegate to parent implementation for uncached files
        super.checkDelete(file);
        // If we get here, permission was granted, so cache the result
        deletePermissionCache.put(file, Boolean.TRUE);
    }

    @Override
    public void checkExec(String cmd) {
        // Check if we've already verified this file
        if (execPermissionCache.containsKey(cmd)) {
            // Permission was granted, so we can return immediately
            return;
        }

        // Delegate to parent implementation for uncached files
        super.checkExec(cmd);
        // If we get here, permission was granted, so cache the result
        execPermissionCache.put(cmd, Boolean.TRUE);
    }
}