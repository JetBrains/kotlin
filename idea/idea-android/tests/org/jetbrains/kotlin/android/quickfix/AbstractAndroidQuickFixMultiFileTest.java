/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.quickfix;

import com.intellij.codeInsight.ImportFilter;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.impl.FacetUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.Extensions;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixMultiFileTest;
import org.jetbrains.kotlin.idea.test.KotlinTestImportFilter;

public abstract class AbstractAndroidQuickFixMultiFileTest extends AbstractQuickFixMultiFileTest {

    @Override
    protected void setUp() {
        super.setUp();
        addAndroidFacet();
        Extensions.getRootArea().getExtensionPoint(ImportFilter.EP_NAME).registerExtension(KotlinTestImportFilter.INSTANCE);
    }

    @Override
    protected void tearDown() {
        try {
            Extensions.getRootArea().getExtensionPoint(ImportFilter.EP_NAME).unregisterExtension(KotlinTestImportFilter.INSTANCE);
            AndroidFacet facet = FacetManager.getInstance(myModule).getFacetByType(AndroidFacet.getFacetType().getId());
            FacetUtil.deleteFacet(facet);
        } finally {
            super.tearDown();
        }
    }

    @Override
    protected void doTestWithExtraFile(@NotNull String beforeFileName) {
        addManifest();
        super.doTestWithExtraFile(beforeFileName);
    }

    private void addAndroidFacet() {
        FacetManager facetManager = FacetManager.getInstance(myModule);
        AndroidFacet facet = facetManager.createFacet(AndroidFacet.getFacetType(), "Android", null);

        ModifiableFacetModel facetModel = facetManager.createModifiableModel();
        facetModel.addFacet(facet);
        ApplicationManager.getApplication().runWriteAction(facetModel::commit);
    }

    private void addManifest() {
        myFixture.configureByFile("idea/testData/android/AndroidManifest.xml");
    }
}
