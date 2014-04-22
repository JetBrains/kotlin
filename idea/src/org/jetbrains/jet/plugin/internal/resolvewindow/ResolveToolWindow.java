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

package org.jetbrains.jet.plugin.internal.resolvewindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.inference.BoundsOwner;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionDebugInfo;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.internal.Location;
import org.jetbrains.jet.plugin.util.InfinitePeriodicalTask;
import org.jetbrains.jet.plugin.util.LongRunningReadTask;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionDebugInfo.*;

public class ResolveToolWindow extends JPanel implements Disposable {

    public static final String BAR = "\n\n===\n\n";

    private static final int UPDATE_DELAY = 1000;
    private static final String DEFAULT_TEXT = "/*\n" +
                                               "Information about symbols resolved by\nKotlin compiler.\n" +
                                               "No Kotlin source file is opened.\n" +
                                               "*/";

    private static final String NO_REFERENCE_TEXT = "/*\n" +
                                               "Information about symbols resolved by\nKotlin compiler.\n" +
                                               "Invalid place for getting reference information.\n" +
                                               "*/";

    private class UpdateResolveToolWindowTask extends LongRunningReadTask<Location, String> {
        @Override
        protected Location prepareRequestInfo() {
            if (!toolWindow.isVisible()) {
                return null;
            }

            Location location = Location.fromEditor(FileEditorManager.getInstance(myProject).getSelectedTextEditor(), myProject);
            if (location.getEditor() == null || location.getJetFile() == null) {
                return null;
            }

            return location;
        }

        @Override
        protected void hideResultOnInvalidLocation() {
            setText(DEFAULT_TEXT);
        }

        @NotNull
        @Override
        protected String processRequest(@NotNull Location requestInfo) {
            JetFile jetFile = requestInfo.getJetFile();
            assert jetFile != null;

            int startOffset = requestInfo.getStartOffset();
            int endOffset = requestInfo.getEndOffset();

            BindingContext bindingContext = ResolvePackage.getAnalysisResults(jetFile).getBindingContext();

            PsiElement elementAtOffset;
            if (startOffset == endOffset) {
                elementAtOffset = PsiUtilCore.getElementAtOffset(jetFile, startOffset);
            }
            else {
                PsiElement start = PsiUtilCore.getElementAtOffset(jetFile, startOffset);
                PsiElement end = PsiUtilCore.getElementAtOffset(jetFile, endOffset - 1);
                elementAtOffset = PsiTreeUtil.findCommonParent(start, end);
            }

            PsiElement elementWithDebugInfo = findData(bindingContext, elementAtOffset, RESOLUTION_DEBUG_INFO);
            if (elementWithDebugInfo != null) {
                return renderDebugInfo(elementWithDebugInfo, bindingContext.get(RESOLUTION_DEBUG_INFO, elementWithDebugInfo), null);
            }

            @SuppressWarnings("unchecked")
            PsiElement elementWithResolvedCall = findData(bindingContext, elementAtOffset, (ReadOnlySlice) RESOLVED_CALL);
            if (elementWithResolvedCall instanceof JetElement) {
                return renderDebugInfo(elementWithResolvedCall, null,
                                       bindingContext.get(RESOLVED_CALL, (JetElement) elementWithResolvedCall));
            }

            JetExpression parentExpression = (elementAtOffset instanceof JetExpression) ?
                                             (JetExpression) elementAtOffset :
                                             PsiTreeUtil.getParentOfType(elementAtOffset, JetExpression.class);

            if (parentExpression != null) {
                JetType type = bindingContext.get(EXPRESSION_TYPE, parentExpression);
                String text = parentExpression + "|" + parentExpression.getText() + "| : " + type;
                if (parentExpression instanceof JetReferenceExpression) {
                    JetReferenceExpression referenceExpression = (JetReferenceExpression) parentExpression;
                    DeclarationDescriptor target = bindingContext.get(REFERENCE_TARGET, referenceExpression);
                    text += "\nReference target: \n" + target;
                }

                return text;
            }

            return NO_REFERENCE_TEXT;
        }

        @Override
        protected void onResultReady(@NotNull Location requestInfo, @Nullable String resultText) {
            if (resultText != null) {
                setText(resultText);
            }
        }
    }

    private final Editor myEditor;

    private final Project myProject;
    private final ToolWindow toolWindow;

    public ResolveToolWindow(Project project, ToolWindow toolWindow) {
        super(new BorderLayout());
        myProject = project;
        this.toolWindow = toolWindow;
        myEditor = EditorFactory.getInstance()
                .createEditor(EditorFactory.getInstance().createDocument(""), project, JetFileType.INSTANCE, true);
        add(myEditor.getComponent());

        new InfinitePeriodicalTask(UPDATE_DELAY, Alarm.ThreadToUse.SWING_THREAD, this, new Computable<LongRunningReadTask>() {
            @Override
            public LongRunningReadTask compute() {
                return new UpdateResolveToolWindowTask();
            }
        }).start();

        setText(DEFAULT_TEXT);
    }

    @Nullable
    private static <D> PsiElement findData(BindingContext bindingContext, PsiElement currentElement, ReadOnlySlice<PsiElement, D> slice) {
        while (currentElement != null && !(currentElement instanceof PsiFile)) {
            if (currentElement instanceof JetElement) {
                JetElement atOffset = (JetElement) currentElement;
                D data = bindingContext.get(slice, atOffset);
                if (data != null && data != NO_DEBUG_INFO) {
                    return currentElement;
                }
            }

            currentElement = currentElement.getParent();
        }

        return null;
    }

    @NotNull
    private static String renderDebugInfo(
            PsiElement currentElement,
            @Nullable ResolutionDebugInfo.Data debugInfo,
            @Nullable ResolvedCall<?> call
    ) {
        StringBuilder result = new StringBuilder();

        if (debugInfo != null) {
            for (ResolutionTask<?, ?> resolutionTask : debugInfo.get(TASKS)) {
                for (ResolvedCallWithTrace<?> resolvedCall : resolutionTask.getResolvedCalls()) {
                    renderResolutionLogForCall(debugInfo, resolvedCall, result);
                }
            }

            call = debugInfo.get(RESULT);
        }

        if (call != null) {
            renderCall(result, call);
        }
        else {
            result.append("Resolved call is null\n");
        }
        result.append(currentElement).append(": ").append(currentElement.getText());
        return result.toString();
    }

    private static void renderResolutionLogForCall(Data debugInfo, ResolvedCallWithTrace<?> resolvedCall, StringBuilder result) {
        result.append("Trying to call ").append(resolvedCall.getCandidateDescriptor()).append("\n");
        StringBuilder errors = debugInfo.getByKey(ERRORS, resolvedCall);
        if (errors != null) {
            result.append("Errors: \n").append(errors).append(BAR);
        }

        StringBuilder log = debugInfo.getByKey(LOG, resolvedCall);
        if (log != null) {
            result.append("Log: \n").append(log).append(BAR);
        }

        Map<JetType, BoundsOwner> knowns = debugInfo.getByKey(BOUNDS_FOR_KNOWNS, resolvedCall);
        renderMap(knowns, result);
        Map<TypeParameterDescriptor, BoundsOwner> unknowns = debugInfo.getByKey(BOUNDS_FOR_UNKNOWNS, resolvedCall);
        renderMap(unknowns, result);

        result.append(BAR);
    }

    private static <K> void renderMap(Map<K, BoundsOwner> map, StringBuilder builder) {
        if (map == null) return;

        for (Map.Entry<K, BoundsOwner> entry : map.entrySet()) {
            K key = entry.getKey();
            BoundsOwner typeValue = entry.getValue();
            builder.append("Bounds for ").append(key).append("\n");
            for (BoundsOwner bound : typeValue.getLowerBounds()) {
                builder.append("    >: ").append(bound).append("\n");
            }
            for (BoundsOwner bound : typeValue.getUpperBounds()) {
                builder.append("    <: ").append(bound).append("\n");
            }
        }
    }

    private static String renderCall(StringBuilder builder, ResolvedCall<?> resolvedCall) {
        CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
        ReceiverValue receiverArgument = resolvedCall.getReceiverArgument();
        ReceiverValue thisObject = resolvedCall.getThisObject();
        Map<TypeParameterDescriptor, JetType> typeArguments = resolvedCall.getTypeArguments();
        Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments = resolvedCall.getValueArguments();

        renderReceiver(receiverArgument, thisObject, builder);
        builder.append(resultingDescriptor.getName());
        renderTypeArguments(typeArguments, builder);

        if (resultingDescriptor instanceof FunctionDescriptor) {
            renderValueArguments(valueArguments, builder);
        }

        builder.append(" : ").append(resultingDescriptor.getReturnType());

        builder.append("\n");
        builder.append("\n");

        CallableDescriptor candidateDescriptor = resolvedCall.getCandidateDescriptor();
        builder.append("Candidate: \n").append(candidateDescriptor).append("\n");
        if (resultingDescriptor != candidateDescriptor) {
            builder.append("Result: \n").append(resultingDescriptor).append("\n");
        }

        builder.append("Receiver: \n").append(receiverArgument).append("\n");
        builder.append("This object: \n").append(thisObject).append("\n");
        builder.append("Type args: \n").append(typeArguments).append("\n");
        builder.append("Value args: \n").append(valueArguments).append("\n");

        return builder.toString();
    }

    private static void renderValueArguments(Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments, StringBuilder builder) {
        ResolvedValueArgument[] args = new ResolvedValueArgument[valueArguments.size()];
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : valueArguments.entrySet()) {
            ValueParameterDescriptor key = entry.getKey();
            ResolvedValueArgument value = entry.getValue();

            assert key.getIndex() < args.length: "parameter index " + key.getIndex() + " >= resolved arguments " + args.length + "\n"
                    + valueArguments.toString();
            args[key.getIndex()] = value;
        }
        builder.append("(");
        for (int i = 0, argsLength = args.length; i < argsLength; i++) {
            ResolvedValueArgument arg = args[i];
            builder.append(arg);
            if (i != argsLength - 1) {
                builder.append(", ");
            }
        }
        builder.append(")");
    }

    private static void renderTypeArguments(Map<TypeParameterDescriptor, JetType> typeArguments, StringBuilder builder) {
        JetType[] args = new JetType[typeArguments.size()];
        for (Map.Entry<TypeParameterDescriptor, JetType> entry : typeArguments.entrySet()) {
            TypeParameterDescriptor key = entry.getKey();
            JetType value = entry.getValue();
            args[key.getIndex()] = value;
        }
        builder.append("<");
        for (int i = 0, argsLength = args.length; i < argsLength; i++) {
            JetType type = args[i];
            builder.append(type);
            if (i != argsLength - 1) {
                builder.append(", ");
            }
        }
        builder.append(">");
    }

    private static void renderReceiver(ReceiverValue receiverArgument, ReceiverValue thisObject, StringBuilder builder) {
        if (receiverArgument.exists()) {
            builder.append("/").append(receiverArgument);
        }

        if (thisObject.exists()) {
            builder.append("/this=").append(thisObject);
        }

        if (thisObject.exists() || receiverArgument.exists()) {
            builder.append("/.");
        }
    }

    private void setText(@NotNull final String text) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                myEditor.getDocument().setText(StringUtil.convertLineSeparators(text));
            }
        });
    }

    @Override
    public void dispose() {
        EditorFactory.getInstance().releaseEditor(myEditor);
    }
}
