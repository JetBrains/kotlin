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
        String packageName = stub.getPackageName();
        FqName fqName = new FqName(packageName == null ? "" : packageName);

        while (true) {
            sink.occurrence(JetAllPackagesIndex.getInstance().getKey(), fqName.getFqName());
            if (fqName.isRoot()) {
                return;
            }
            fqName = fqName.parent();
        }
    }

    @Override
    public void indexClass(PsiJetClassStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(JetShortClassNameIndex.getInstance().getKey(), name);
        }
        
        FqName fqn = stub.getFqName();
        if (fqn != null) {
            sink.occurrence(JetFullClassNameIndex.getInstance().getKey(), fqn.getFqName());
        }

        for (String superName : stub.getSuperNames()) {
            sink.occurrence(JetSuperClassIndex.getInstance().getKey(), superName);
        }

        recordClassOrObjectByPackage(stub, sink);
    }

    @Override
    public void indexObject(PsiJetObjectStub stub, IndexSink sink) {
        String name = stub.getName();
        FqName fqName = stub.getFqName();

        if (stub.isClassObject()) {
            StubElement parentStub = stub.getParentStub();
            assert parentStub instanceof PsiJetStubWithFqName<?> : "Something but a class/object is a parent to class object stub: " + parentStub;

            name = JvmAbi.CLASS_OBJECT_CLASS_NAME;

            FqName parentFqName = ((PsiJetStubWithFqName<?>) parentStub).getFqName();
            if (parentFqName != null) {
                fqName = parentFqName.child(Name.identifier(name));
            }
        }
        else {
            assert name != null;
        }

        sink.occurrence(JetShortClassNameIndex.getInstance().getKey(), name);

        if (stub.isTopLevel()) {
            sink.occurrence(JetTopLevelShortObjectNameIndex.getInstance().getKey(), name);
        }

        if (fqName != null) {
            sink.occurrence(JetFullClassNameIndex.getInstance().getKey(), fqName.getFqName());
        }

        recordClassOrObjectByPackage(stub, sink);
    }

    private static void recordClassOrObjectByPackage(StubElement<? extends JetClassOrObject> stub, IndexSink sink) {
        StubElement parentStub = stub.getParentStub();
        if (parentStub instanceof PsiJetFileStub) {
            PsiJetFileStub jetFileStub = (PsiJetFileStub) parentStub;
            String packageName = jetFileStub.getPackageName();
            if (packageName != null) {
                sink.occurrence(JetClassByPackageIndex.getInstance().getKey(), packageName);
            }
        }
    }

    @Override
    public void indexFunction(PsiJetFunctionStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            if (stub.isTopLevel()) {
                // Collection only top level functions as only they are expected in completion without explicit import
                if (!stub.isExtension()) {
                    sink.occurrence(JetShortFunctionNameIndex.getInstance().getKey(), name);
                }
                else {
                    sink.occurrence(JetExtensionFunctionNameIndex.getInstance().getKey(), name);
                }

                FqName topFQName = stub.getTopFQName();
                if (topFQName != null) {
                    sink.occurrence(JetTopLevelFunctionsFqnNameIndex.getInstance().getKey(), topFQName.getFqName());
                }
            }

            sink.occurrence(JetAllShortFunctionNameIndex.getInstance().getKey(), name);
        }
    }

    @Override
    public void indexProperty(PsiJetPropertyStub stub, IndexSink sink) {
        String propertyName = stub.getName();
        if (propertyName != null) {
            if (stub.isTopLevel()) {
                FqName topFQName = stub.getTopFQName();
                if (topFQName != null) {
                    sink.occurrence(JetTopLevelPropertiesFqnNameIndex.getInstance().getKey(), topFQName.getFqName());
                }
            }

            sink.occurrence(JetShortPropertiesNameIndex.getInstance().getKey(), propertyName);
        }
    }

    @Override
    public void indexAnnotation(PsiJetAnnotationStub stub, IndexSink sink) {
        sink.occurrence(JetAnnotationsIndex.getInstance().getKey(), stub.getShortName());
    }
}
