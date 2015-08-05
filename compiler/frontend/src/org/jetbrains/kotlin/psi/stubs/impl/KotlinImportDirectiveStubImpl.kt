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

package org.jetbrains.kotlin.psi.stubs.impl;

import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.JetImportDirective;
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

public class KotlinImportDirectiveStubImpl extends KotlinStubBaseImpl<JetImportDirective> implements KotlinImportDirectiveStub {
    private final boolean isAbsoluteInRootPackage;
    private final boolean isAllUnder;
    @Nullable
    private final StringRef aliasName;
    private final boolean isValid;

    public KotlinImportDirectiveStubImpl(
            StubElement parent,
            boolean isAbsoluteInRootPackage,
            boolean isAllUnder,
            @Nullable StringRef aliasName,
            boolean isValid
    ) {
        super(parent, JetStubElementTypes.IMPORT_DIRECTIVE);
        this.isAbsoluteInRootPackage = isAbsoluteInRootPackage;
        this.isAllUnder = isAllUnder;
        this.aliasName = aliasName;
        this.isValid = isValid;
    }

    @Override
    public boolean isAbsoluteInRootPackage() {
        return isAbsoluteInRootPackage;
    }

    @Override
    public boolean isAllUnder() {
        return isAllUnder;
    }

    @Nullable
    @Override
    public String getAliasName() {
        return StringRef.toString(aliasName);
    }

    @Override
    public boolean isValid() {
        return isValid;
    }
}
