/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.framework;

import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FrameworksCompatibilityUtils {
    private FrameworksCompatibilityUtils() {
    }

    public static void suggestRemoveIncompatibleFramework(
            @NotNull ModifiableRootModel rootModel,
            @NotNull LibraryKind kind,
            String message,
            String title
    ) {
        List<OrderEntry> existingEntries = new ArrayList<OrderEntry>();

        for (OrderEntry entry : rootModel.getOrderEntries()) {
            if (!(entry instanceof LibraryOrderEntry)) continue;
            final Library library = ((LibraryOrderEntry)entry).getLibrary();
            if (library == null) continue;

            if (LibraryPresentationManager.getInstance().isLibraryOfKind(Arrays.asList(library.getFiles(OrderRootType.CLASSES)), kind)) {
                existingEntries.add(entry);
            }
        }

        if (!existingEntries.isEmpty()) {
            int result = Messages.showYesNoDialog(message, title, Messages.getWarningIcon());

            if (result == 0) {
                for (OrderEntry entry : existingEntries) {
                    rootModel.removeOrderEntry(entry);
                }
            }
        }
    }
}
