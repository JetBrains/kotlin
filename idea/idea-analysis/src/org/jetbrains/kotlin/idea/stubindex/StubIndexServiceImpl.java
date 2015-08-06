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

package org.jetbrains.kotlin.idea.stubindex;

import com.intellij.psi.stubs.IndexSink;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.stubs.*;
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService;
import org.jetbrains.kotlin.util.UtilPackage;

import java.util.List;

public class StubIndexServiceImpl implements StubIndexService {

    @Override
    public void indexFile(KotlinFileStub stub, IndexSink sink) {
        FqName packageFqName = stub.getPackageFqName();

        sink.occurrence(JetExactPackagesIndex.getInstance().getKey(), packageFqName.asString());

        JetFile psi = stub.getPsi();
        if (psi != null) {
            FqName staticFacadeFqName = PackagePartClassUtils.getPackagePartFqName(psi);
            sink.occurrence(JetStaticFacadeClassIndex.INSTANCE.getKey(), staticFacadeFqName.asString());
        }
    }

    @Override
    public void indexClass(KotlinClassStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(JetClassShortNameIndex.getInstance().getKey(), name);
        }

        FqName fqName = stub.getFqName();
        if (fqName != null) {
            sink.occurrence(JetFullClassNameIndex.getInstance().getKey(), fqName.asString());

            if (stub.isTopLevel()) {
                sink.occurrence(JetTopLevelClassByPackageIndex.getInstance().getKey(), fqName.parent().asString());
            }
        }

        indexSuperNames(stub, sink);
    }

    @Override
    public void indexObject(KotlinObjectStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(JetClassShortNameIndex.getInstance().getKey(), name);
        }

        FqName fqName = stub.getFqName();
        if (fqName != null) {
            sink.occurrence(JetFullClassNameIndex.getInstance().getKey(), fqName.asString());

            if (stub.isTopLevel()) {
                sink.occurrence(JetTopLevelClassByPackageIndex.getInstance().getKey(), fqName.parent().asString());
            }
        }

        indexSuperNames(stub, sink);
    }

    private static void indexSuperNames(KotlinClassOrObjectStub<? extends JetClassOrObject> stub, IndexSink sink) {
        for (String superName : stub.getSuperNames()) {
            sink.occurrence(JetSuperClassIndex.getInstance().getKey(), superName);
        }
    }

    @Override
    public void indexFunction(KotlinFunctionStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(JetFunctionShortNameIndex.getInstance().getKey(), name);

            if (UtilPackage.isProbablyNothing(stub.getPsi().getTypeReference())) {
                sink.occurrence(JetProbablyNothingFunctionShortNameIndex.getInstance().getKey(), name);
            }
        }

        if (stub.isTopLevel()) {
            // can have special fq name in case of syntactically incorrect function with no name
            FqName fqName = stub.getFqName();
            if (fqName != null) {
                sink.occurrence(JetTopLevelFunctionFqnNameIndex.getInstance().getKey(), fqName.asString());
                sink.occurrence(JetTopLevelFunctionByPackageIndex.getInstance().getKey(), fqName.parent().asString());
                StubindexPackage.indexTopLevelExtension(stub, sink);
            }
        }
    }

    @Override
    public void indexProperty(KotlinPropertyStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(JetPropertyShortNameIndex.getInstance().getKey(), name);

            if (UtilPackage.isProbablyNothing(stub.getPsi().getTypeReference())) {
                sink.occurrence(JetProbablyNothingPropertyShortNameIndex.getInstance().getKey(), name);
            }
        }

        if (stub.isTopLevel()) {
            FqName fqName = stub.getFqName();
            // can have special fq name in case of syntactically incorrect property with no name
            if (fqName != null) {
                sink.occurrence(JetTopLevelPropertyFqnNameIndex.getInstance().getKey(), fqName.asString());
                sink.occurrence(JetTopLevelPropertyByPackageIndex.getInstance().getKey(), fqName.parent().asString());
                StubindexPackage.indexTopLevelExtension(stub, sink);
            }
        }
    }

    @Override
    public void indexAnnotation(KotlinAnnotationEntryStub stub, IndexSink sink) {
        sink.occurrence(JetAnnotationsIndex.getInstance().getKey(), stub.getShortName());

        KotlinFileStub fileStub = getContainingFileStub(stub);
        if (fileStub != null) {
            List<KotlinImportDirectiveStub> aliasImportStubs = fileStub.findImportsByAlias(stub.getShortName());
            for (KotlinImportDirectiveStub importStub : aliasImportStubs) {
                sink.occurrence(JetAnnotationsIndex.getInstance().getKey(), importStub.getImportedFqName().shortName().asString());
            }
        }
    }

    private static KotlinFileStub getContainingFileStub(StubElement stub) {
        StubElement parent = stub.getParentStub();
        while (parent != null) {
            if (parent instanceof KotlinFileStub) {
                return (KotlinFileStub) parent;
            }
            parent = parent.getParentStub();
        }
        return null;
    }
}
