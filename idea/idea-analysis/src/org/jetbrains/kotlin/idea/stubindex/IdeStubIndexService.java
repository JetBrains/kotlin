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
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.stubs.*;
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService;
import org.jetbrains.kotlin.util.TypeIndexUtilKt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IdeStubIndexService extends StubIndexService {

    @Override
    public void indexFile(KotlinFileStub stub, IndexSink sink) {
        FqName packageFqName = stub.getPackageFqName();

        sink.occurrence(KotlinExactPackagesIndex.getInstance().getKey(), packageFqName.asString());

        FqName facadeFqName = ((KotlinFileStubForIde) stub).getFacadeFqName();
        if (facadeFqName != null) {
            sink.occurrence(KotlinFileFacadeFqNameIndex.INSTANCE.getKey(), facadeFqName.asString());
            sink.occurrence(KotlinFileFacadeShortNameIndex.INSTANCE.getKey(), facadeFqName.shortName().asString());
            sink.occurrence(KotlinFileFacadeClassByPackageIndex.INSTANCE.getKey(), packageFqName.asString());
        }

        FqName partFqName = ((KotlinFileStubForIde) stub).getPartFqName();
        if (partFqName != null) {
            sink.occurrence(KotlinFilePartClassIndex.INSTANCE.getKey(), partFqName.asString());
        }

        List<StringRef> partNames = ((KotlinFileStubForIde) stub).getFacadePartSimpleNames();
        if (partNames != null) {
            for (StringRef partName : partNames) {
                String partSimpleName = StringRef.toString(partName);
                if (partSimpleName == null) {
                    continue;
                }
                FqName multifileClassPartFqName = packageFqName.child(Name.identifier(partSimpleName));
                sink.occurrence(KotlinMultifileClassPartIndex.INSTANCE.getKey(), multifileClassPartFqName.asString());
            }
        }
    }

    @Override
    public void indexClass(KotlinClassStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(KotlinClassShortNameIndex.getInstance().getKey(), name);
        }

        FqName fqName = stub.getFqName();
        if (fqName != null) {
            sink.occurrence(KotlinFullClassNameIndex.getInstance().getKey(), fqName.asString());

            if (stub.isTopLevel()) {
                sink.occurrence(KotlinTopLevelClassByPackageIndex.getInstance().getKey(), fqName.parent().asString());
            }
        }

        if (stub.isInterface()) {
            sink.occurrence(KotlinClassShortNameIndex.getInstance().getKey(), JvmAbi.DEFAULT_IMPLS_CLASS_NAME);
        }

        indexSuperNames(stub, sink);
    }

    @Override
    public void indexObject(KotlinObjectStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(KotlinClassShortNameIndex.getInstance().getKey(), name);
        }

        FqName fqName = stub.getFqName();
        if (fqName != null) {
            sink.occurrence(KotlinFullClassNameIndex.getInstance().getKey(), fqName.asString());

            if (stub.isTopLevel()) {
                sink.occurrence(KotlinTopLevelClassByPackageIndex.getInstance().getKey(), fqName.parent().asString());
            }
        }

        indexSuperNames(stub, sink);
    }

    private static void indexSuperNames(KotlinClassOrObjectStub<? extends KtClassOrObject> stub, IndexSink sink) {
        for (String superName : stub.getSuperNames()) {
            sink.occurrence(KotlinSuperClassIndex.getInstance().getKey(), superName);
        }
    }

    @Override
    public void indexFunction(KotlinFunctionStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(KotlinFunctionShortNameIndex.getInstance().getKey(), name);

            if (TypeIndexUtilKt.isProbablyNothing(stub.getPsi().getTypeReference())) {
                sink.occurrence(KotlinProbablyNothingFunctionShortNameIndex.getInstance().getKey(), name);
            }
        }

        if (stub.isTopLevel()) {
            // can have special fq name in case of syntactically incorrect function with no name
            FqName fqName = stub.getFqName();
            if (fqName != null) {
                sink.occurrence(KotlinTopLevelFunctionFqnNameIndex.getInstance().getKey(), fqName.asString());
                sink.occurrence(KotlinTopLevelFunctionByPackageIndex.getInstance().getKey(), fqName.parent().asString());
                IndexUtilsKt.indexTopLevelExtension(stub, sink);
            }
        }
    }

    @Override
    public void indexProperty(KotlinPropertyStub stub, IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(KotlinPropertyShortNameIndex.getInstance().getKey(), name);

            if (TypeIndexUtilKt.isProbablyNothing(stub.getPsi().getTypeReference())) {
                sink.occurrence(KotlinProbablyNothingPropertyShortNameIndex.getInstance().getKey(), name);
            }
        }

        if (stub.isTopLevel()) {
            FqName fqName = stub.getFqName();
            // can have special fq name in case of syntactically incorrect property with no name
            if (fqName != null) {
                sink.occurrence(KotlinTopLevelPropertyFqnNameIndex.getInstance().getKey(), fqName.asString());
                sink.occurrence(KotlinTopLevelPropertyByPackageIndex.getInstance().getKey(), fqName.parent().asString());
                IndexUtilsKt.indexTopLevelExtension(stub, sink);
            }
        }
    }

    @Override
    public void indexAnnotation(KotlinAnnotationEntryStub stub, IndexSink sink) {
        sink.occurrence(KotlinAnnotationsIndex.getInstance().getKey(), stub.getShortName());

        KotlinFileStub fileStub = getContainingFileStub(stub);
        if (fileStub != null) {
            List<KotlinImportDirectiveStub> aliasImportStubs = fileStub.findImportsByAlias(stub.getShortName());
            for (KotlinImportDirectiveStub importStub : aliasImportStubs) {
                sink.occurrence(KotlinAnnotationsIndex.getInstance().getKey(), importStub.getImportedFqName().shortName().asString());
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

    @NotNull
    @Override
    public KotlinFileStub createFileStub(@NotNull KtFile file) {
        StringRef packageFqName = StringRef.fromString(file.getPackageFqNameByTree().asString());
        boolean isScript = file.isScriptByTree();
        if (PackagePartClassUtils.fileHasTopLevelCallables(file)) {
            JvmFileClassInfo fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(file);
            StringRef facadeSimpleName = StringRef.fromString(fileClassInfo.getFacadeClassFqName().shortName().asString());
            StringRef partSimpleName = StringRef.fromString(fileClassInfo.getFileClassFqName().shortName().asString());
            return new KotlinFileStubForIde(file, packageFqName, isScript, facadeSimpleName, partSimpleName, null);
        }
        return new KotlinFileStubForIde(file, packageFqName, isScript, null, null, null);
    }

    @Override
    public void serializeFileStub(
            @NotNull KotlinFileStub stub, @NotNull StubOutputStream dataStream
    ) throws IOException {
        KotlinFileStubForIde fileStub = (KotlinFileStubForIde) stub;
        dataStream.writeName(fileStub.getPackageFqName().asString());
        dataStream.writeBoolean(fileStub.isScript());
        dataStream.writeName(StringRef.toString(fileStub.getFacadeSimpleName()));
        dataStream.writeName(StringRef.toString(fileStub.getPartSimpleName()));
        List<StringRef> facadePartNames = fileStub.getFacadePartSimpleNames();
        if (facadePartNames == null) {
            dataStream.writeInt(0);
        }
        else {
            dataStream.writeInt(facadePartNames.size());
            for (StringRef partName : facadePartNames) {
                dataStream.writeName(StringRef.toString(partName));
            }
        }
    }

    @NotNull
    @Override
    public KotlinFileStub deserializeFileStub(@NotNull StubInputStream dataStream) throws IOException {
        StringRef packageFqNameAsString = dataStream.readName();
        boolean isScript = dataStream.readBoolean();
        StringRef facadeSimpleName = dataStream.readName();
        StringRef partSimpleName = dataStream.readName();
        int numPartNames = dataStream.readInt();
        List<StringRef> facadePartNames = new ArrayList<StringRef>();
        for (int i = 0; i < numPartNames; ++i) {
            StringRef partNameRef = dataStream.readName();
            facadePartNames.add(partNameRef);
        }
        return new KotlinFileStubForIde(null, packageFqNameAsString, isScript, facadeSimpleName, partSimpleName, facadePartNames);
    }
}
