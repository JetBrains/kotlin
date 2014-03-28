/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi.stubs.impl;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.stubs.PsiJetImportDirectiveStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;

public class PsiJetImportDirectiveStubImpl extends StubBase<JetImportDirective> implements PsiJetImportDirectiveStub {
    private final boolean isAbsoluteInRootPackage;
    private final boolean isAllUnder;
    @Nullable
    private final StringRef aliasName;

    public PsiJetImportDirectiveStubImpl(
            StubElement parent,
            boolean isAbsoluteInRootPackage,
            boolean isAllUnder,
            @Nullable StringRef aliasName
    ) {
        super(parent, JetStubElementTypes.IMPORT_DIRECTIVE);
        this.isAbsoluteInRootPackage = isAbsoluteInRootPackage;
        this.isAllUnder = isAllUnder;
        this.aliasName = aliasName;
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
}
