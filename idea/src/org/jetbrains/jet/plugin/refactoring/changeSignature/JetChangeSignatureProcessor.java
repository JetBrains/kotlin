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

package org.jetbrains.jet.plugin.refactoring.changeSignature;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessorBase;
import com.intellij.refactoring.changeSignature.ChangeSignatureUsageProcessor;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.refactoring.ui.ConflictsDialog;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.containers.HashSet;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JetChangeSignatureProcessor extends ChangeSignatureProcessorBase {
    private final String commandName;

    public JetChangeSignatureProcessor(Project project, JetChangeInfo changeInfo, String commandName) {
        super(project, changeInfo);
        this.commandName = commandName;
    }

    @NotNull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(UsageInfo[] usages) {
        String subject = getChangeInfo().isConstructor() ? "constructor" : "function";
        return new JetUsagesViewDescriptor(myChangeInfo.getMethod(), RefactoringBundle.message("0.to.change.signature", subject));
    }

    @Override
    public JetChangeInfo getChangeInfo() {
        return (JetChangeInfo) super.getChangeInfo();
    }

    @Override
    protected boolean preprocessUsages(Ref<UsageInfo[]> refUsages) {
        for (ChangeSignatureUsageProcessor processor : ChangeSignatureUsageProcessor.EP_NAME.getExtensions()) {
            if (!processor.setupDefaultValues(myChangeInfo, refUsages, myProject)) return false;
        }
        MultiMap<PsiElement, String> conflictDescriptions = new MultiMap<PsiElement, String>();
        for (ChangeSignatureUsageProcessor usageProcessor : ChangeSignatureUsageProcessor.EP_NAME.getExtensions()) {
            MultiMap<PsiElement, String> conflicts = usageProcessor.findConflicts(myChangeInfo, refUsages);
            for (PsiElement key : conflicts.keySet()) {
                Collection<String> collection = conflictDescriptions.get(key);
                if (collection.size() == 0) collection = new HashSet<String>();
                collection.addAll(conflicts.get(key));
                conflictDescriptions.put(key, collection);
            }
        }

        UsageInfo[] usagesIn = refUsages.get();
        RenameUtil.addConflictDescriptions(usagesIn, conflictDescriptions);
        Set<UsageInfo> usagesSet = new HashSet<UsageInfo>(Arrays.asList(usagesIn));
        RenameUtil.removeConflictUsages(usagesSet);
        if (!conflictDescriptions.isEmpty()) {
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                throw new ConflictsInTestsException(conflictDescriptions.values());
            }
            if (myPrepareSuccessfulSwingThreadCallback != null) {
                ConflictsDialog dialog = prepareConflictsDialog(conflictDescriptions, usagesIn);
                dialog.show();
                if (!dialog.isOK()) {
                    if (dialog.isShowConflicts()) prepareSuccessful();
                    return false;
                }
            }
        }

        UsageInfo[] array = usagesSet.toArray(new UsageInfo[usagesSet.size()]);
        Arrays.sort(array, new Comparator<UsageInfo>() {
            @Override
            public int compare(@NotNull UsageInfo u1, @NotNull UsageInfo u2) {
                PsiElement element1 = u1.getElement();
                PsiElement element2 = u2.getElement();
                int rank1 = element1 != null ? element1.getTextOffset() : -1;
                int rank2 = element2 != null ? element2.getTextOffset() : -1;
                return rank2 - rank1; // Reverse order
            }
        });
        refUsages.set(array);
        prepareSuccessful();
        return true;
    }

    @Override
    protected boolean isPreviewUsages(UsageInfo[] usages) {
        return isPreviewUsages();
    }

    @NotNull
    @Override
    protected Collection<? extends PsiElement> getElementsToWrite(@NotNull UsageViewDescriptor descriptor) {
        Collection<PsiElement> elements = new ArrayList<PsiElement>();
        elements.addAll(super.getElementsToWrite(descriptor));
        elements.addAll(getChangeInfo().getGeneratedInfo().getFilesToWrite());
        return elements;
    }

    @Override
    protected String getCommandName() {
        return commandName;
    }
}
