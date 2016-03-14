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

package org.jetbrains.kotlin.idea.framework;

import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;

public class JavaFrameworkType extends FrameworkTypeEx {
    public static JavaFrameworkType getInstance() {
        return FrameworkTypeEx.EP_NAME.findExtension(JavaFrameworkType.class);
    }

    public JavaFrameworkType() {
        super("kotlin-java-framework-id");
    }

    @NotNull
    @Override
    public FrameworkSupportInModuleProvider createProvider() {
        return new JavaFrameworkSupportProvider();
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return "Kotlin (Java)";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return KotlinIcons.SMALL_LOGO;
    }
}
