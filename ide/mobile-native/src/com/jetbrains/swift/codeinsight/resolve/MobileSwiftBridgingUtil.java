package com.jetbrains.swift.codeinsight.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext;
import com.jetbrains.cidr.lang.psi.OCFile;
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable;
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration;
import com.jetbrains.cidr.xcode.Xcode;
import com.jetbrains.swift.languageKind.SwiftLanguageKind;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class MobileSwiftBridgingUtil {
    @NotNull
    static SwiftGlobalSymbols buildBridgedSymbols(@NotNull SwiftModule module) {
        List<VirtualFile> headers = module.getBridgingHeaders();
        if (headers.isEmpty()) return SwiftGlobalSymbols.EMPTY;

        OCResolveConfiguration configuration = module.getConfiguration();
        Project project = configuration.getProject();
        PsiManager psiManager = PsiManager.getInstance(project);

        SwiftGlobalSymbolsImpl bridgedSymbols = new SwiftGlobalSymbolsImpl(SwiftGlobalSymbols.SymbolsOrigin.OBJC);

        SwiftGlobalSymbolsImpl.SymbolProcessor processor = new SwiftGlobalSymbolsImpl.SymbolProcessor(bridgedSymbols, false);

        for (VirtualFile header : headers) {
            if (!header.isValid()) continue;
            PsiFile file = psiManager.findFile(header);
            if (!(file instanceof OCFile)) continue;

            OCInclusionContext context = OCInclusionContext.beforePCHFileContext(configuration, SwiftLanguageKind.INSTANCE, file);
            FileSymbolTable table = FileSymbolTable.forFile(header, context);
            if (table == null) continue;

            FileSymbolTable.ProcessingState state = new FileSymbolTable.ProcessingState(context, false) {
                @Override
                public boolean startProcessing(@NotNull FileSymbolTable table) {
                    if (shouldSkipBuildingBridgedSymbols(table.getContainingFile(), project)) {
                        return false;
                    }
                    return super.startProcessing(table);
                }
            };
            table.processSymbols(processor, null, state, null, null, null);
        }

        if (!processor.isAnyProcessed()) return SwiftGlobalSymbols.EMPTY;
        bridgedSymbols.compact();
        return bridgedSymbols;
    }

    @Contract("null, _ -> true")
    private static boolean shouldSkipBuildingBridgedSymbols(@Nullable VirtualFile file, @NotNull Project project) {
        if (file == null) return true;

        VirtualFile xcodeRoot = CachedValuesManager.getManager(project).getCachedValue(project, () -> CachedValueProvider.Result.create(
                LocalFileSystem.getInstance().findFileByPath(Xcode.getBasePath()),
                ModificationTracker.NEVER_CHANGED
        ));

        if (xcodeRoot == null) return false;
        return VfsUtilCore.isAncestor(xcodeRoot, file, true);
    }
}
