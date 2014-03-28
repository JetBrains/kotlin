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

package org.jetbrains.jet.plugin.stubindex;

import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.stubs.*;
import org.jetbrains.jet.lang.psi.stubs.elements.StubIndexService;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public class StubIndexServiceImpl implements StubIndexService {

    @Override
    public void indexFile(PsiJetFileStub stub, IndexSink sink) {
        FqName packageFqName = stub.getPackageFqName();

        while (true) {
            sink.occurrence(JetAllPackagesIndex.getInstance().getKey(), packageFqName.asString());
            if (packageFqName.isRoot()) {
                return;
            }
            packageFqName = packageFqName.parent();
        }
    }

    @Override
    public void indexClass(PsiJetClassStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(JetClassShortNameIndex.getInstance().getKey(), name);
        }

        FqName fqn = stub.getFqName();
        if (fqn != null) {
            sink.occurrence(JetFullClassNameIndex.getInstance().getKey(), fqn.asString());
        }

        indexSuperNames(stub, sink);
        recordClassOrObjectByPackage(stub, sink);
    }

    @Override
    public void indexObject(PsiJetObjectStub stub, IndexSink sink) {
        String name = stub.getName();
        FqName fqName = stub.getFqName();

        if (stub.isClassObject()) {
            StubElement parentClassStub = stub.getParentStub().getParentStub().getParentStub();
            assert parentClassStub instanceof PsiJetStubWithFqName<?>
                    : "Something but a class/object is a parent to class object stub: " + parentClassStub;

            name = JvmAbi.CLASS_OBJECT_CLASS_NAME;

            FqName parentFqName = ((PsiJetStubWithFqName<?>) parentClassStub).getFqName();
            if (parentFqName != null) {
                fqName = parentFqName.child(Name.identifier(name));
            }
        }

        if (name != null) {
            sink.occurrence(JetClassShortNameIndex.getInstance().getKey(), name);

            if (stub.isTopLevel()) {
                sink.occurrence(JetTopLevelObjectShortNameIndex.getInstance().getKey(), name);
            }
        }

        if (fqName != null) {
            sink.occurrence(JetFullClassNameIndex.getInstance().getKey(), fqName.asString());
        }

        indexSuperNames(stub, sink);
        recordClassOrObjectByPackage(stub, sink);
    }

    private static void indexSuperNames(PsiJetClassOrObjectStub<? extends JetClassOrObject> stub, IndexSink sink) {
        for (String superName : stub.getSuperNames()) {
            sink.occurrence(JetSuperClassIndex.getInstance().getKey(), superName);
        }
    }

    private static void recordClassOrObjectByPackage(StubElement<? extends JetClassOrObject> stub, IndexSink sink) {
        StubElement parentStub = stub.getParentStub();
        if (parentStub instanceof PsiJetFileStub) {
            PsiJetFileStub jetFileStub = (PsiJetFileStub) parentStub;
            FqName packageFqName = jetFileStub.getPackageFqName();
            sink.occurrence(JetClassByPackageIndex.getInstance().getKey(), packageFqName.asString());
        }
    }

    @Override
    public void indexFunction(PsiJetFunctionStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            if (stub.isTopLevel()) {
                // Collection only top level functions as only they are expected in completion without explicit import
                if (!stub.isExtension()) {
                    sink.occurrence(JetTopLevelNonExtensionFunctionShortNameIndex.getInstance().getKey(), name);
                }
                else {
                    sink.occurrence(JetTopLevelExtensionFunctionShortNameIndex.getInstance().getKey(), name);
                }
            }
            sink.occurrence(JetFunctionShortNameIndex.getInstance().getKey(), name);
        }
        // can have special fq name in case of syntactically incorrect function with no name
        FqName topFQName = stub.getFqName();
        if (topFQName != null) {
            sink.occurrence(JetTopLevelFunctionsFqnNameIndex.getInstance().getKey(), topFQName.asString());
        }
    }

    @Override
    public void indexProperty(PsiJetPropertyStub stub, IndexSink sink) {
        String propertyName = stub.getName();
        if (propertyName != null) {
            sink.occurrence(JetPropertyShortNameIndex.getInstance().getKey(), propertyName);
        }
        // can have special fq name in case of syntactically incorrect function with no name
        if (stub.isTopLevel()) {
            FqName topFQName = stub.getFqName();
            if (topFQName != null) {
                sink.occurrence(JetTopLevelPropertiesFqnNameIndex.getInstance().getKey(), topFQName.asString());
            }
        }
    }

    @Override
    public void indexAnnotation(PsiJetAnnotationEntryStub stub, IndexSink sink) {
        sink.occurrence(JetAnnotationsIndex.getInstance().getKey(), stub.getShortName());
    }
}
