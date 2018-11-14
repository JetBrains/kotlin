/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.gradle;

import com.intellij.codeInspection.LocalInspectionEP;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;

public class KotlinNativeIdeInitializer implements ApplicationComponent {

    @Override
    public void initComponent() {
        // TODO: Move this to Kotlin/Native plugin for CLion and AppCode (see KT-26717):
        //unregisterGroovyInspections();
    }

    // There are groovy local inspections which should not be loaded w/o groovy plugin enabled.
    // Those plugin definitions should become optional and dependant on groovy plugin.
    // This is a temp workaround before it happens.
    private static void unregisterGroovyInspections() {
        ExtensionPoint<LocalInspectionEP> extensionPoint =
                Extensions.getRootArea().getExtensionPoint(LocalInspectionEP.LOCAL_INSPECTION);

        for (LocalInspectionEP ep : extensionPoint.getExtensions()) {
            if ("Kotlin".equals(ep.groupDisplayName) && "Groovy".equals(ep.language)) {
                extensionPoint.unregisterExtension(ep);
            }
        }
    }
}
