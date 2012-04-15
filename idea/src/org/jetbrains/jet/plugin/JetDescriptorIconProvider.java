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

package org.jetbrains.jet.plugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;

import javax.swing.*;

/**
 * @author Nikolay Krasko
 */
public final class JetDescriptorIconProvider {

    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.plugin.compiler.JetCompiler");

    private JetDescriptorIconProvider() {
    }

    public static Icon getIcon(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof NamespaceDescriptor) {
            return PlatformIcons.PACKAGE_OPEN_ICON;
        }
        if (descriptor instanceof FunctionDescriptor) {
            return JetIcons.FUNCTION;
        }
        if (descriptor instanceof ClassDescriptor) {
            switch (((ClassDescriptor)descriptor).getKind()) {
                case TRAIT:
                    return JetIcons.TRAIT;
                case ENUM_CLASS:
                    return PlatformIcons.ENUM_ICON;
                case ENUM_ENTRY:
                    return PlatformIcons.ENUM_ICON;
                case ANNOTATION_CLASS:
                    return PlatformIcons.ANNOTATION_TYPE_ICON;
                case OBJECT:
                    return JetIcons.OBJECT;
                case CLASS:
                    return PlatformIcons.CLASS_ICON;
                default:
                    LOG.warn("No icon for descriptor: " + descriptor);
                    return null;
            }
        }

        if (descriptor instanceof PropertyDescriptor) {
            return ((PropertyDescriptor)descriptor).isVar() ? JetIcons.VAR : JetIcons.VAL;
        }

        LOG.warn("No icon for descriptor: " + descriptor);
        return null;
    }
}
