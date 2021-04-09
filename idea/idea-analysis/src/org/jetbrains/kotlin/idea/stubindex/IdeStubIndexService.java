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

package org.jetbrains.kotlin.idea.stubindex;

import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.stubs.*;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService;
import org.jetbrains.kotlin.util.TypeIndexUtilKt;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class IdeStubIndexService extends StubIndexService {

    @Override
    public void indexFile(@NotNull KotlinFileStub stub, @NotNull IndexSink sink) {
        FqName packageFqName = stub.getPackageFqName();

        sink.occurrence(KotlinExactPackagesIndex.getInstance().getKey(), packageFqName.asString());

        if (stub.isScript()) return;

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
                FqName multiFileClassPartFqName = packageFqName.child(Name.identifier(partSimpleName));
                sink.occurrence(KotlinMultifileClassPartIndex.INSTANCE.getKey(), multiFileClassPartFqName.asString());
            }
        }
    }

    @Override
    public void indexClass(@NotNull KotlinClassStub stub, @NotNull IndexSink sink) {
        processNames(sink, stub.getName(), stub.getFqName(), stub.isTopLevel());

        if (stub.isInterface()) {
            sink.occurrence(KotlinClassShortNameIndex.getInstance().getKey(), JvmAbi.DEFAULT_IMPLS_CLASS_NAME);
        }

        indexSuperNames(stub, sink);
    }

    @Override
    public void indexObject(@NotNull KotlinObjectStub stub, @NotNull IndexSink sink) {
        processNames(sink, stub.getName(), stub.getFqName(), stub.isTopLevel());

        indexSuperNames(stub, sink);
    }

    private static void processNames(
            @NotNull IndexSink sink,
            String shortName,
            FqName fqName,
            boolean level) {
        if (shortName != null) {
            sink.occurrence(KotlinClassShortNameIndex.getInstance().getKey(), shortName);
        }

        if (fqName != null) {
            sink.occurrence(KotlinFullClassNameIndex.getInstance().getKey(), fqName.asString());

            if (level) {
                sink.occurrence(KotlinTopLevelClassByPackageIndex.getInstance().getKey(), fqName.parent().asString());
            }
        }
    }

    private static void indexSuperNames(KotlinClassOrObjectStub<? extends KtClassOrObject> stub, IndexSink sink) {
        for (String superName : stub.getSuperNames()) {
            sink.occurrence(KotlinSuperClassIndex.getInstance().getKey(), superName);
        }

        if (!(stub instanceof KotlinClassStub)) {
            return;
        }

        KotlinModifierListStub modifierListStub = getModifierListStub(stub);
        if (modifierListStub == null) return;

        if (modifierListStub.hasModifier(KtTokens.ENUM_KEYWORD)) {
            sink.occurrence(KotlinSuperClassIndex.getInstance().getKey(), Enum.class.getSimpleName());
        }
        if (modifierListStub.hasModifier(KtTokens.ANNOTATION_KEYWORD)) {
            sink.occurrence(KotlinSuperClassIndex.getInstance().getKey(), Annotation.class.getSimpleName());
        }
    }

    @Nullable
    private static KotlinModifierListStub getModifierListStub(@NotNull KotlinClassOrObjectStub<? extends KtClassOrObject> stub) {
        return stub.findChildStubByType(KtStubElementTypes.MODIFIER_LIST);
    }

    @Override
    public void indexFunction(@NotNull KotlinFunctionStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(KotlinFunctionShortNameIndex.getInstance().getKey(), name);

            if (TypeIndexUtilKt.isProbablyNothing(stub.getPsi().getTypeReference())) {
                sink.occurrence(KotlinProbablyNothingFunctionShortNameIndex.getInstance().getKey(), name);
            }

            if (stub.mayHaveContract()) {
                sink.occurrence(KotlinProbablyContractedFunctionShortNameIndex.getInstance().getKey(), name);
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

        IndexUtilsKt.indexInternals(stub, sink);
    }

    @Override
    public void indexTypeAlias(@NotNull KotlinTypeAliasStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(KotlinTypeAliasShortNameIndex.getInstance().getKey(), name);
        }

        IndexUtilsKt.indexTypeAliasExpansion(stub, sink);

        FqName fqName = stub.getFqName();
        if (fqName != null) {
            if (stub.isTopLevel()) {
                sink.occurrence(KotlinTopLevelTypeAliasFqNameIndex.getInstance().getKey(), fqName.asString());
                sink.occurrence(KotlinTopLevelTypeAliasByPackageIndex.getInstance().getKey(), fqName.parent().asString());
            }
        }

        ClassId classId = stub.getClassId();
        if (classId != null && !stub.isTopLevel()) {
            sink.occurrence(KotlinInnerTypeAliasClassIdIndex.getInstance().getKey(), classId.asString());
        }
    }

    @Override
    public void indexProperty(@NotNull KotlinPropertyStub stub, @NotNull IndexSink sink) {
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

        IndexUtilsKt.indexInternals(stub, sink);
    }

    @Override
    public void indexParameter(@NotNull KotlinParameterStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null && stub.hasValOrVar()) {
            sink.occurrence(KotlinPropertyShortNameIndex.getInstance().getKey(), name);
        }
    }

    @Override
    public void indexAnnotation(@NotNull KotlinAnnotationEntryStub stub, @NotNull IndexSink sink) {
        String name = stub.getShortName();
        if (name == null) {
            return;
        }
        sink.occurrence(KotlinAnnotationsIndex.getInstance().getKey(), name);

        KotlinFileStub fileStub = getContainingFileStub(stub);
        if (fileStub != null) {
            List<KotlinImportDirectiveStub> aliasImportStubs = fileStub.findImportsByAlias(name);
            for (KotlinImportDirectiveStub importStub : aliasImportStubs) {
                FqName importedFqName = importStub.getImportedFqName();
                if (importedFqName != null) {
                    sink.occurrence(KotlinAnnotationsIndex.getInstance().getKey(), importedFqName.shortName().asString());
                }
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

    @Override
    public void indexScript(@NotNull KotlinScriptStub stub, @NotNull IndexSink sink) {
        sink.occurrence(KotlinScriptFqnIndex.getInstance().getKey(), stub.getFqName().asString());
    }

    @NotNull
    @Override
    public KotlinFileStub createFileStub(@NotNull KtFile file) {
        StringRef packageFqName = StringRef.fromString(file.getPackageFqNameByTree().asString());
        boolean isScript = file.isScriptByTree();
        if (file.hasTopLevelCallables()) {
            JvmFileClassInfo fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(file);
            StringRef facadeFqNameRef = StringRef.fromString(fileClassInfo.getFacadeClassFqName().asString());
            StringRef partSimpleName = StringRef.fromString(fileClassInfo.getFileClassFqName().shortName().asString());
            return new KotlinFileStubForIde(file, packageFqName, isScript, facadeFqNameRef, partSimpleName, null);
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
        FqName facadeFqName = fileStub.getFacadeFqName();
        dataStream.writeName(facadeFqName != null ? facadeFqName.asString() : null);
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
        if (packageFqNameAsString == null) {
            throw new IllegalStateException("Can't read package fqname from stream");
        }

        boolean isScript = dataStream.readBoolean();
        StringRef facadeStringRef = dataStream.readName();
        StringRef partSimpleName = dataStream.readName();
        int numPartNames = dataStream.readInt();
        List<StringRef> facadePartNames = new ArrayList<>();
        for (int i = 0; i < numPartNames; ++i) {
            StringRef partNameRef = dataStream.readName();
            facadePartNames.add(partNameRef);
        }
        return new KotlinFileStubForIde(null, packageFqNameAsString, isScript, facadeStringRef, partSimpleName, facadePartNames);
    }
}
