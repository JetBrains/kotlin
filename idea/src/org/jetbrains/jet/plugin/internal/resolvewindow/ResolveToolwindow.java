/*
 * @author max
 */
package org.jetbrains.jet.plugin.internal.resolvewindow;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.ResolutionDebugInfo;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.inference.BoundsOwner;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.internal.codewindow.BytecodeToolwindow;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.calls.ResolutionDebugInfo.*;

/*
 * @author abreslav
 */
public class ResolveToolwindow extends JPanel {
    public static class Factory implements ToolWindowFactory {
        @Override
        public void createToolWindowContent(Project project, ToolWindow toolWindow) {
            toolWindow.getContentManager().addContent(ContentFactory.SERVICE.getInstance().createContent(new ResolveToolwindow(project), "", false));
        }
    }

    private static final int UPDATE_DELAY = 500;
    private final Editor myEditor;
    private final Alarm myUpdateAlarm;
    private BytecodeToolwindow.Location myCurrentLocation;
    private final Project myProject;


    public ResolveToolwindow(Project project) {
        super(new BorderLayout());
        myProject = project;
        myEditor = EditorFactory.getInstance().createEditor(EditorFactory.getInstance().createDocument(""), project, JetFileType.INSTANCE, true);
        add(myEditor.getComponent());
        myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
        myUpdateAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
                myUpdateAlarm.addRequest(this, UPDATE_DELAY);
                BytecodeToolwindow.Location location = BytecodeToolwindow.Location.fromEditor(FileEditorManager.getInstance(myProject).getSelectedTextEditor());
                if (!Comparing.equal(location, myCurrentLocation)) {
                    render(location, myCurrentLocation);
                    myCurrentLocation = location;
                }
            }
        }, UPDATE_DELAY);
    }

    private void render(BytecodeToolwindow.Location location, BytecodeToolwindow.Location oldLocation) {
        Editor editor = location.getEditor();
        if (editor == null) {
            setText("No editor");
        }
        else {
            VirtualFile vFile = ((EditorEx) editor).getVirtualFile();
            if (vFile == null) {
                setText("");
                return;
            }

            PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vFile);
            if (!(psiFile instanceof JetFile)) {
                setText("");
                return;
            }


            int startOffset = location.getStartOffset();
            int endOffset = location.getEndOffset();
            if (oldLocation == null || !Comparing.equal(oldLocation.getEditor(), location.getEditor())
                    || oldLocation.getStartOffset() != startOffset
                    || oldLocation.getEndOffset() != endOffset) {

                BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) psiFile);

                
                PsiElement elementAtOffset;
                if (startOffset == endOffset) {
                    elementAtOffset = PsiUtilCore.getElementAtOffset(psiFile, startOffset);
                }
                else {
                    PsiElement start = PsiUtilCore.getElementAtOffset(psiFile, startOffset);
                    PsiElement end = PsiUtilCore.getElementAtOffset(psiFile, endOffset - 1);
                    elementAtOffset = PsiTreeUtil.findCommonParent(start, end);
                }

                PsiElement currentElement = elementAtOffset;

                boolean callFound = false;

                PsiElement elementWithDebugInfo = findData(bindingContext, currentElement, RESOLUTION_DEBUG_INFO);
                if (elementWithDebugInfo != null) {
                    callFound = true;
                    setText(renderDebugInfo(elementWithDebugInfo, bindingContext.get(RESOLUTION_DEBUG_INFO, elementWithDebugInfo), null));
                }
                else {
                    PsiElement elementWithResolvedCall = findData(bindingContext, currentElement, (WritableSlice) RESOLVED_CALL);
                    if (elementWithResolvedCall instanceof JetElement) {
                        callFound = true;
                        setText(renderDebugInfo(elementWithResolvedCall, null, bindingContext.get(RESOLVED_CALL, (JetElement) elementWithResolvedCall)));
                    }
                }

                if (!callFound) {

                    JetExpression parentExpression = (elementAtOffset instanceof JetExpression) ? (JetExpression) elementAtOffset
                                                                                                : PsiTreeUtil.getParentOfType(elementAtOffset, JetExpression.class);
                    if (parentExpression != null) {
                        JetType type = bindingContext.get(EXPRESSION_TYPE, parentExpression);
                        String text = parentExpression + "|" + parentExpression.getText() + "| : " + type;
                        if (parentExpression instanceof JetReferenceExpression) {
                            JetReferenceExpression referenceExpression = (JetReferenceExpression) parentExpression;
                            DeclarationDescriptor target = bindingContext.get(REFERENCE_TARGET, referenceExpression);
                            text += "\nReference target: \n" + target;
                        }
                        setText(text);
                    }
                }
            }
        }
    }

    @Nullable
    private <D> PsiElement findData(BindingContext bindingContext, PsiElement currentElement, ReadOnlySlice<PsiElement, D> slice) {
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

    private String renderDebugInfo(PsiElement currentElement, @Nullable ResolutionDebugInfo.Data debugInfo, @Nullable ResolvedCall<? extends CallableDescriptor> call) {
        final String bar = "\n\n===\n\n";

        StringBuilder result = new StringBuilder();

        if (debugInfo != null) {
            StringBuilder errors = debugInfo.get(ERRORS);
            if (errors != null) {
                result.append("Errors: \n").append(errors).append(bar);
            }

            StringBuilder log = debugInfo.get(LOG);
            if (log != null) {
                result.append("Log: \n").append(log).append(bar);
            }

            Map<JetType, BoundsOwner> knowns = debugInfo.get(BOUNDS_FOR_KNOWNS);
            renderMap(knowns, result);
            Map<TypeParameterDescriptor, BoundsOwner> unknowns = debugInfo.get(BOUNDS_FOR_UNKNOWNS);
            renderMap(unknowns, result);

            result.append(bar);

            call = debugInfo.get(RESULT);
        }

        renderCall(result, call);
        result.append(currentElement + ": " + currentElement.getText());
        return result.toString();
    }
    
    private <K> void renderMap(Map<K, BoundsOwner> map, StringBuilder builder) {
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

    private String renderCall(StringBuilder builder, ResolvedCall<? extends CallableDescriptor> resolvedCall) {

        CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
        ReceiverDescriptor receiverArgument = resolvedCall.getReceiverArgument();
        ReceiverDescriptor thisObject = resolvedCall.getThisObject();
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

    private void renderValueArguments(Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments, StringBuilder builder) {
        ResolvedValueArgument[] args = new ResolvedValueArgument[valueArguments.size()];
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : valueArguments.entrySet()) {
            ValueParameterDescriptor key = entry.getKey();
            ResolvedValueArgument value = entry.getValue();

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

    private void renderTypeArguments(Map<TypeParameterDescriptor, JetType> typeArguments, StringBuilder builder) {
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

    private void renderReceiver(ReceiverDescriptor receiverArgument, ReceiverDescriptor thisObject, StringBuilder builder) {
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

    private void setText(final String text) {
        new WriteCommandAction(myProject) {
            @Override
            protected void run(Result result) throws Throwable {
                myEditor.getDocument().setText(text);
            }
        }.execute();
    }
}
