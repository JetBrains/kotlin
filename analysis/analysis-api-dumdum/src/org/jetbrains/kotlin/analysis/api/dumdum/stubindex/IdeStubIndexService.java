// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.api.dumdum.stubindex;

import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.stubs.*;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class IdeStubIndexService extends StubIndexService {

    @Override
    public void indexFile(@NotNull KotlinFileStub stub, @NotNull IndexSink sink) {
        FqName packageFqName = stub.getPackageFqName();

        sink.occurrence(KotlinExactPackagesIndex.NAME, packageFqName.asString());

        if (stub.isScript()) return;

        FqName facadeFqName = ((KotlinFileStubImpl) stub).getFacadeFqName();
        if (facadeFqName != null) {
            sink.occurrence(KotlinFileFacadeFqNameIndex.Helper.getIndexKey(), facadeFqName.asString());
            sink.occurrence(KotlinFileFacadeShortNameIndex.Helper.getIndexKey(), facadeFqName.shortName().asString());
            sink.occurrence(KotlinFileFacadeClassByPackageIndex.Helper.getIndexKey(), packageFqName.asString());
        }

        FqName partFqName = ((KotlinFileStubImpl) stub).getPartFqName();
        if (partFqName != null) {
            sink.occurrence(KotlinFilePartClassIndex.Helper.getIndexKey(), partFqName.asString());
        }

        List<String> partNames = ((KotlinFileStubImpl) stub).getFacadePartSimpleNames();
        if (partNames != null) {
            for (String partName : partNames) {
                if (partName == null) {
                    continue;
                }
                FqName multiFileClassPartFqName = packageFqName.child(Name.identifier(partName));
                sink.occurrence(KotlinMultiFileClassPartIndex.Helper.getIndexKey(), multiFileClassPartFqName.asString());
            }
        }
    }

    @Override
    public void indexClass(@NotNull KotlinClassStub stub, @NotNull IndexSink sink) {
        processNames(sink, stub.getName(), stub.getFqName(), stub.isTopLevel());

        if (stub.isInterface()) {
            sink.occurrence(KotlinClassShortNameIndex.Helper.getIndexKey(), JvmAbi.DEFAULT_IMPLS_CLASS_NAME);
        }

        indexSuperNames(stub, sink);

        indexPrime(stub, sink);
    }

    /**
     * Indexes non-private top-level symbols or members of top-level objects and companion objects subject to this object serving as namespaces.
     */
    private static void indexPrime(KotlinStubWithFqName<?> stub, IndexSink sink) {
        String name = stub.getName();
        if (name == null) return;

        KotlinModifierListStub modifierList = getModifierListStub(stub);
        if (modifierList != null && modifierList.hasModifier(KtTokens.PRIVATE_KEYWORD)) return;
        if (modifierList != null && modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return;

        StubElement<?> parent = stub.getParentStub();
        boolean prime = false;
        if (parent instanceof KotlinFileStub) {
            prime = true;
        }
        else if (parent instanceof KotlinObjectStub) {
            StubElement<?> grand = parent.getParentStub();
            boolean primeGrand = grand instanceof KotlinClassStub && ((KotlinClassStub) grand).isTopLevel();

            prime = ((KotlinObjectStub) parent).isTopLevel() ||
                    primeGrand && ((KotlinObjectStub) parent).isCompanion();
        }

        if (prime) {
            sink.occurrence(KotlinPrimeSymbolNameIndex.Helper.getIndexKey(), name);
        }
    }

    @Override
    public void indexObject(@NotNull KotlinObjectStub stub, @NotNull IndexSink sink) {
        String shortName = stub.getName();
        processNames(sink, shortName, stub.getFqName(), stub.isTopLevel());

        indexSuperNames(stub, sink);

        indexPrime(stub, sink);

        if (shortName != null && !stub.isObjectLiteral() && !stub.getSuperNames().isEmpty()) {
            sink.occurrence(KotlinSubclassObjectNameIndex.Helper.getIndexKey(), shortName);
        }
    }

    private static void processNames(
            @NotNull IndexSink sink,
            String shortName,
            FqName fqName,
            boolean level) {
        if (shortName != null) {
            sink.occurrence(KotlinClassShortNameIndex.Helper.getIndexKey(), shortName);
        }

        if (fqName != null) {
            sink.occurrence(KotlinFullClassNameIndex.Helper.getIndexKey(), fqName.asString());

            if (level) {
                sink.occurrence(KotlinTopLevelClassByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());
            }
        }
    }

    private static void indexSuperNames(KotlinClassOrObjectStub<? extends KtClassOrObject> stub, IndexSink sink) {
        for (String superName : stub.getSuperNames()) {
            sink.occurrence(KotlinSuperClassIndex.Helper.getIndexKey(), superName);
        }

        if (!(stub instanceof KotlinClassStub)) {
            return;
        }

        KotlinModifierListStub modifierListStub = getModifierListStub(stub);
        if (modifierListStub == null) return;

        if (modifierListStub.hasModifier(KtTokens.ENUM_KEYWORD)) {
            sink.occurrence(KotlinSuperClassIndex.Helper.getIndexKey(), Enum.class.getSimpleName());
        }
        if (modifierListStub.hasModifier(KtTokens.ANNOTATION_KEYWORD)) {
            sink.occurrence(KotlinSuperClassIndex.Helper.getIndexKey(), Annotation.class.getSimpleName());
        }
    }

    @Nullable
    private static KotlinModifierListStub getModifierListStub(@NotNull KotlinStubWithFqName<?> stub) {
        return stub.findChildStubByType(KtStubElementTypes.MODIFIER_LIST);
    }

    @Override
    public void indexFunction(@NotNull KotlinFunctionStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(KotlinFunctionShortNameIndex.Helper.getIndexKey(), name);

            if (IndexUtilsKt.isDeclaredInObject(stub)) {
                IndexUtilsKt.indexExtensionInObject(stub, sink);
            }

            KtTypeReference typeReference = stub.getPsi().getTypeReference();
            if (typeReference != null && KotlinPsiHeuristics.isProbablyNothing(typeReference)) {
                sink.occurrence(KotlinProbablyNothingFunctionShortNameIndex.Helper.getIndexKey(), name);
            }

            if (stub.mayHaveContract()) {
                sink.occurrence(KotlinProbablyContractedFunctionShortNameIndex.Helper.getIndexKey(), name);
            }

            indexPrime(stub, sink);
        }

        if (stub.isTopLevel()) {
            // can have special fq name in case of syntactically incorrect function with no name
            FqName fqName = stub.getFqName();
            if (fqName != null) {
                KtNamedFunction ktNamedFunction = stub.getPsi();
                if (KotlinPsiUtils.isExpectDeclaration(ktNamedFunction)) {
                    sink.occurrence(KotlinTopLevelExpectFunctionFqNameIndex.Helper.getIndexKey(), fqName.asString());
                }

                sink.occurrence(KotlinTopLevelFunctionFqnNameIndex.Helper.getIndexKey(), fqName.asString());
                sink.occurrence(KotlinTopLevelFunctionByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());
                IndexUtilsKt.indexTopLevelExtension(stub, sink);
            }
        }

        IndexUtilsKt.indexInternals(stub, sink);
    }

    @Override
    public void indexTypeAlias(@NotNull KotlinTypeAliasStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(KotlinTypeAliasShortNameIndex.Helper.getIndexKey(), name);
            indexPrime(stub, sink);
        }

        IndexUtilsKt.indexTypeAliasExpansion(stub, sink);

        FqName fqName = stub.getFqName();
        if (fqName != null) {
            if (stub.isTopLevel()) {
                sink.occurrence(KotlinTopLevelTypeAliasFqNameIndex.Helper.getIndexKey(), fqName.asString());
                sink.occurrence(KotlinTopLevelTypeAliasByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());
            }
        }

        ClassId classId = stub.getClassId();
        if (classId != null && !stub.isTopLevel()) {
            sink.occurrence(KotlinInnerTypeAliasClassIdIndex.Helper.getIndexKey(), classId.asString());
        }
    }

    @Override
    public void indexProperty(@NotNull KotlinPropertyStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null) {
            sink.occurrence(KotlinPropertyShortNameIndex.Helper.getIndexKey(), name);

            if (IndexUtilsKt.isDeclaredInObject(stub)) {
                IndexUtilsKt.indexExtensionInObject(stub, sink);
            }

            KtTypeReference typeReference = stub.getPsi().getTypeReference();
            if (typeReference != null && KotlinPsiHeuristics.isProbablyNothing(typeReference)) {
                sink.occurrence(KotlinProbablyNothingPropertyShortNameIndex.Helper.getIndexKey(), name);
            }
            indexPrime(stub, sink);
        }

        if (stub.isTopLevel()) {
            FqName fqName = stub.getFqName();
            // can have special fq name in case of syntactically incorrect property with no name
            if (fqName != null) {
                KtProperty ktProperty = stub.getPsi();
                if (KotlinPsiUtils.isExpectDeclaration(ktProperty)) {
                    sink.occurrence(KotlinTopLevelExpectPropertyFqNameIndex.Helper.getIndexKey(), fqName.asString());
                }

                sink.occurrence(KotlinTopLevelPropertyFqnNameIndex.Helper.getIndexKey(), fqName.asString());
                sink.occurrence(KotlinTopLevelPropertyByPackageIndex.Helper.getIndexKey(), fqName.parent().asString());
                IndexUtilsKt.indexTopLevelExtension(stub, sink);
            }
        }

        IndexUtilsKt.indexInternals(stub, sink);
    }

    @Override
    public void indexParameter(@NotNull KotlinParameterStub stub, @NotNull IndexSink sink) {
        String name = stub.getName();
        if (name != null && stub.hasValOrVar()) {
            sink.occurrence(KotlinPropertyShortNameIndex.Helper.getIndexKey(), name);
        }
    }

    @Override
    public void indexAnnotation(@NotNull KotlinAnnotationEntryStub stub, @NotNull IndexSink sink) {
        String name = stub.getShortName();
        if (name == null) {
            return;
        }
        sink.occurrence(KotlinAnnotationsIndex.Helper.getIndexKey(), name);

        KotlinFileStub fileStub = KotlinStubUtils.getContainingKotlinFileStub(stub);
        if (fileStub != null) {
            List<KotlinImportDirectiveStub> aliasImportStubs = fileStub.findImportsByAlias(name);
            for (KotlinImportDirectiveStub importStub : aliasImportStubs) {
                FqName importedFqName = importStub.getImportedFqName();
                if (importedFqName != null) {
                    sink.occurrence(KotlinAnnotationsIndex.Helper.getIndexKey(), importedFqName.shortName().asString());
                }
            }
        }

        IndexUtilsKt.indexJvmNameAnnotation(stub, sink);
    }

    @Override
    public void indexScript(@NotNull KotlinScriptStub stub, @NotNull IndexSink sink) {
        sink.occurrence(KotlinScriptFqnIndex.Helper.getIndexKey(), stub.getFqName().asString());
    }

    @NotNull
    @Override
    public KotlinFileStub createFileStub(@NotNull KtFile file) {
        String packageFqName =file.getPackageFqNameByTree().asString();
        boolean isScript = file.isScriptByTree();
        if (file.hasTopLevelCallables()) {
            JvmFileClassInfo fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(file);
            String facadeFqNameRef = fileClassInfo.getFacadeClassFqName().asString();
            String partSimpleName = fileClassInfo.getFileClassFqName().shortName().asString();
            return new KotlinFileStubImpl(file, packageFqName, isScript, facadeFqNameRef, partSimpleName, null);
        }
        return new KotlinFileStubImpl(file, packageFqName, isScript, null, null, null);
    }

    @Override
    public void serializeFileStub(
            @NotNull KotlinFileStub stub, @NotNull StubOutputStream dataStream
    ) throws IOException {
        KotlinFileStubImpl fileStub = (KotlinFileStubImpl) stub;
        dataStream.writeName(fileStub.getPackageFqName().asString());
        dataStream.writeBoolean(fileStub.isScript());
        FqName facadeFqName = fileStub.getFacadeFqName();
        dataStream.writeName(facadeFqName != null ? facadeFqName.asString() : null);
        dataStream.writeName(fileStub.getPartSimpleName());
        List<String> facadePartNames = fileStub.getFacadePartSimpleNames();
        if (facadePartNames == null) {
            dataStream.writeInt(0);
        }
        else {
            dataStream.writeInt(facadePartNames.size());
            for (String partName : facadePartNames) {
                dataStream.writeName(partName);
            }
        }
    }

    @NotNull
    @Override
    public KotlinFileStub deserializeFileStub(@NotNull StubInputStream dataStream) throws IOException {
        String packageFqNameAsString = dataStream.readNameString();
        if (packageFqNameAsString == null) {
            throw new IllegalStateException("Can't read package fqname from stream");
        }

        boolean isScript = dataStream.readBoolean();
        String facadeString = dataStream.readNameString();
        String partSimpleName = dataStream.readNameString();
        int numPartNames = dataStream.readInt();
        List<String> facadePartNames = new ArrayList<>();
        for (int i = 0; i < numPartNames; ++i) {
            String partNameRef = dataStream.readNameString();
            facadePartNames.add(partNameRef);
        }
        return new KotlinFileStubImpl(null, packageFqNameAsString, isScript, facadeString, partSimpleName, facadePartNames);
    }
}
