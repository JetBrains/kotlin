/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.*;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.impl.DebuggerUtilsImpl;
import com.intellij.debugger.impl.PositionUtil;
import com.intellij.debugger.jdi.ClassesByNameProvider;
import com.intellij.debugger.jdi.MethodBytecodeUtil;
import com.intellij.debugger.requests.Requestor;
import com.intellij.debugger.ui.breakpoints.*;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.Key;
import com.intellij.psi.*;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.DocumentUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointListener;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.request.*;
import one.util.streamex.StreamEx;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaMethodBreakpointProperties;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.asJava.classes.KtLightClass;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtPrimaryConstructor;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

// This class is copied from com.intellij.debugger.ui.breakpoints.MethodBreakpoint.
// Changed parts are marked with '// MODIFICATION: ' comments.
// This should be deleted when IDEA opens the method breakpoint API (presumably in 193).
public class KotlinFunctionBreakpoint extends BreakpointWithHighlighter<JavaMethodBreakpointProperties> implements MethodBreakpointBase {
    private static final Logger LOG = Logger.getInstance(KotlinFunctionBreakpoint.class);
    @Nullable private JVMName mySignature;
    private boolean myIsStatic;

    protected KotlinFunctionBreakpoint(@NotNull Project project, XBreakpoint breakpoint) {
        super(project, breakpoint);
    }

    public boolean isStatic() {
        return myIsStatic;
    }

    @Override
    @NotNull
    public Key<MethodBreakpoint> getCategory() {
        return MethodBreakpoint.CATEGORY;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && getMethodName() != null;
    }

    // MODIFICATION: Start Kotlin implementation
    @Override
    public void reload() {
        super.reload();

        setMethodName(null);
        mySignature = null;

        Project project = myProject;

        Task.Backgroundable task = new Task.Backgroundable(myProject, "Initialize function breakpoint") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                SourcePosition sourcePosition = KotlinFunctionBreakpoint.this.getSourcePosition();
                MethodDescriptor descriptor = sourcePosition == null
                        ? null : ReadAction.compute(() -> getMethodDescriptor(project, sourcePosition));

                ProgressIndicatorProvider.checkCanceled();

                String methodName = descriptor == null ? null : descriptor.methodName;
                JVMName methodSignature = descriptor == null ? null : descriptor.methodSignature;
                boolean methodIsStatic = descriptor != null && descriptor.isStatic;

                PsiClass psiClass = KotlinFunctionBreakpoint.this.getPsiClass();

                ProgressIndicatorProvider.checkCanceled();

                ApplicationManager.getApplication().invokeLater(() -> {
                    KotlinFunctionBreakpoint.this.setMethodName(methodName);
                    KotlinFunctionBreakpoint.this.mySignature = methodSignature;
                    KotlinFunctionBreakpoint.this.myIsStatic = methodIsStatic;

                    if (psiClass != null) {
                        KotlinFunctionBreakpoint.this.getProperties().myClassPattern = psiClass.getQualifiedName();
                    }
                    if (methodIsStatic) {
                        KotlinFunctionBreakpoint.this.setInstanceFiltersEnabled(false);
                    }
                }, ModalityState.defaultModalityState());
            }
        };

        ProgressManager progressManager = ProgressManager.getInstance();
        if (ApplicationManager.getApplication().isDispatchThread() && !ApplicationManager.getApplication().isUnitTestMode()) {
            progressManager.runProcessWithProgressAsynchronously(task, new BackgroundableProcessIndicator(task));
        } else {
            EmptyProgressIndicator progressIndicator = new EmptyProgressIndicator();
            progressManager.runProcess(() -> task.run(progressIndicator), progressIndicator);
        }
    }

    @Override
    public PsiClass getPsiClass() {
        SourcePosition sourcePosition = getSourcePosition();
        KtClassOrObject declaration = PositionUtil.getPsiElementAt(myProject, KtClassOrObject.class, sourcePosition);

        if (declaration == null) {
            return null;
        }

        return ReadAction.compute(() -> LightClassUtilsKt.toLightClass(declaration));
    }

    // MODIFICATION: End Kotlin implementation

    private static void createRequestForSubClasses(@NotNull MethodBreakpointBase breakpoint,
            @NotNull DebugProcessImpl debugProcess,
            @NotNull ReferenceType baseType) {
        DebuggerManagerThreadImpl.assertIsManagerThread();
        RequestManagerImpl requestsManager = debugProcess.getRequestsManager();
        ClassPrepareRequest request = requestsManager.createClassPrepareRequest((debuggerProcess, referenceType) -> {
            if (instanceOf(referenceType, baseType)) {
                createRequestForPreparedClassEmulated(breakpoint, debugProcess, referenceType, false);
            }
        }, null);
        if (request != null) {
            requestsManager.registerRequest(breakpoint, request);
            request.enable();
        }

        AtomicReference<ProgressWindow> indicatorRef = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(
                () -> {
                    ProgressWindow progress =
                            new ProgressWindow(true, false, debugProcess.getProject(), "Cancel emulation");
                    progress.setDelayInMillis(2000);
                    indicatorRef.set(progress);
                });
        ProgressWindow indicator = indicatorRef.get();

        AtomicBoolean changed = new AtomicBoolean();
        XBreakpointListener<XBreakpoint<?>> listener = new XBreakpointListener<XBreakpoint<?>>() {
            void changed(@NotNull XBreakpoint b) {
                if (b == breakpoint.getXBreakpoint()) {
                    changed.set(true);
                    indicator.cancel();
                }
            }

            @Override
            public void breakpointRemoved(@NotNull XBreakpoint b) {
                changed(b);
            }

            @Override
            public void breakpointChanged(@NotNull XBreakpoint b) {
                changed(b);
            }
        };

        BreakpointListenerConnector.subscribe(debugProcess, indicator, listener);
        ProgressManager.getInstance().executeProcessUnderProgress(
                () -> processPreparedSubTypes(baseType,
                                              (subType, classesByName) ->
                                                      createRequestForPreparedClassEmulated(breakpoint, debugProcess, subType, classesByName, false),
                                              indicator),
                indicator);
        if (indicator.isCanceled() && !changed.get()) {
            breakpoint.disableEmulation();
        }
    }

    @Override
    public void disableEmulation() {
        MethodBreakpointBase.disableEmulation(this);
    }

    private static void createRequestForPreparedClassEmulated(
            @NotNull MethodBreakpointBase breakpoint,
            @NotNull DebugProcessImpl debugProcess,
            @NotNull ReferenceType classType,
            boolean base
    ) {
        createRequestForPreparedClassEmulated(breakpoint, debugProcess, classType, debugProcess.getVirtualMachineProxy().getClassesByNameProvider(), base);
    }

    private static boolean shouldCreateRequest(
            Requestor requestor,
            XBreakpoint xBreakpoint,
            DebugProcessImpl debugProcess,
            boolean forPreparedClass
    ) {
        return ReadAction.compute(() -> {
            JavaDebugProcess process = debugProcess.getXdebugProcess();
            return process != null
                   && debugProcess.isAttached()
                   && (xBreakpoint == null || ((XDebugSessionImpl)process.getSession()).isBreakpointActive(xBreakpoint))
                   && (forPreparedClass || debugProcess.getRequestsManager().findRequests(requestor).isEmpty());
        });
    }

    private static void createRequestForPreparedClassEmulated(
            @NotNull MethodBreakpointBase breakpoint,
            @NotNull DebugProcessImpl debugProcess,
            @NotNull ReferenceType classType,
            @NotNull ClassesByNameProvider classesByName,
            boolean base
    ) {
        if (!MethodBreakpointBase.canBeEmulated(debugProcess)) {
            breakpoint.disableEmulation();
            return;
        }
        if (!base && !shouldCreateRequest(breakpoint, breakpoint.getXBreakpoint(), debugProcess, true)) {
            return;
        }
        Method lambdaMethod = MethodBytecodeUtil.getLambdaMethod(classType, classesByName);
        if (lambdaMethod != null &&
            !breakpoint
                    .matchingMethods(StreamEx.of(((ClassType)classType).interfaces()).flatCollection(ReferenceType::allMethods), debugProcess)
                    .findFirst().isPresent()) {
            return;
        }
        StreamEx<Method> methods = lambdaMethod != null
                                   ? StreamEx.of(lambdaMethod)
                                   : breakpoint.matchingMethods(StreamEx.of(classType.methods()).filter(m -> base || !m.isAbstract()), debugProcess);
        boolean found = false;
        for (Method method : methods) {
            found = true;
            if (method.isNative()) {
                breakpoint.disableEmulation();
                return;
            }
            Method target = MethodBytecodeUtil.getBridgeTargetMethod(method, classesByName);
            if (target != null && !ContainerUtil.isEmpty(DebuggerUtilsEx.allLineLocations(target))) {
                method = target;
            }

            List<Location> allLineLocations = DebuggerUtilsEx.allLineLocations(method);
            if (allLineLocations == null && !method.isBridge()) { // no line numbers
                breakpoint.disableEmulation();
                return;
            }
            if (!ContainerUtil.isEmpty(allLineLocations)) {
                if (breakpoint.isWatchEntry()) {
                    createLocationBreakpointRequest(breakpoint, ContainerUtil.getFirstItem(allLineLocations), debugProcess, true);
                }
                if (breakpoint.isWatchExit()) {
                    MethodBytecodeUtil.visit(method, new MethodVisitor(Opcodes.API_VERSION) {
                        int myLastLine = 0;
                        @Override
                        public void visitLineNumber(int line, Label start) {
                            myLastLine = line;
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            switch (opcode) {
                                case Opcodes.RETURN:
                                case Opcodes.IRETURN:
                                case Opcodes.FRETURN:
                                case Opcodes.ARETURN:
                                case Opcodes.LRETURN:
                                case Opcodes.DRETURN:
                                    //case Opcodes.ATHROW:
                                    allLineLocations.stream()
                                            .filter(l -> l.lineNumber() == myLastLine)
                                            .findFirst().ifPresent(location -> createLocationBreakpointRequest(breakpoint, location, debugProcess, false));
                            }
                        }
                    }, true);
                }
            }
        }
        if (base && found) {
            // desired class found - now also track all new classes
            createRequestForSubClasses(breakpoint, debugProcess, classType);
        }
    }

    private static void createLocationBreakpointRequest(@NotNull FilteredRequestor requestor,
            @Nullable Location location,
            @NotNull DebugProcessImpl debugProcess,
            boolean methodEntry) {
        BreakpointRequest request = createLocationBreakpointRequest(requestor, location, debugProcess);
        if (request != null) {
            request.putProperty(METHOD_ENTRY_KEY, methodEntry);
        }
    }

    @Nullable
    private static BreakpointRequest createLocationBreakpointRequest(
            @NotNull FilteredRequestor requestor,
            @Nullable Location location,
            @NotNull DebugProcessImpl debugProcess
    ) {
        if (location != null) {
            RequestManagerImpl requestsManager = debugProcess.getRequestsManager();
            BreakpointRequest request = requestsManager.createBreakpointRequest(requestor, location);
            requestsManager.enableRequest(request);
            return request;
        }
        return null;
    }

    @Override
    protected void createRequestForPreparedClass(@NotNull DebugProcessImpl debugProcess, @NotNull ReferenceType classType) {
        if (isEmulated()) {
            createRequestForPreparedClassEmulated(this, debugProcess, classType, true);
        }
        else {
            createRequestForPreparedClassOriginal(debugProcess, classType);
        }
    }

    private void createRequestForPreparedClassOriginal(@NotNull DebugProcessImpl debugProcess, @NotNull ReferenceType classType) {
        try {
            boolean hasMethod = false;
            for (Method method : classType.allMethods()) {
                String signature = method.signature();
                String name = method.name();

                String nameFromProperties = getMethodName();
                if (nameFromProperties != null && mySignature != null) {
                    if (nameFromProperties.equals(name) && mySignature.getName(debugProcess).equals(signature)) {
                        hasMethod = true;
                        break;
                    }
                }
            }

            if(!hasMethod) {
                debugProcess.getRequestsManager().setInvalid(
                        this, DebuggerBundle.message("error.invalid.breakpoint.method.not.found", classType.name())
                );
                return;
            }

            RequestManagerImpl requestManager = debugProcess.getRequestsManager();
            if (isWatchEntry()) {
                MethodEntryRequest entryRequest = findRequest(debugProcess, MethodEntryRequest.class, this);
                if (entryRequest == null) {
                    entryRequest = requestManager.createMethodEntryRequest(this);
                }
                else {
                    entryRequest.disable();
                }
                //entryRequest.addClassFilter(myClassQualifiedName);
                // use addClassFilter(ReferenceType) in order to stop on subclasses also!
                entryRequest.addClassFilter(classType);
                debugProcess.getRequestsManager().enableRequest(entryRequest);
            }
            if (isWatchExit()) {
                MethodExitRequest exitRequest = findRequest(debugProcess, MethodExitRequest.class, this);
                if (exitRequest == null) {
                    exitRequest = requestManager.createMethodExitRequest(this);
                }
                else {
                    exitRequest.disable();
                }
                //exitRequest.addClassFilter(myClassQualifiedName);
                exitRequest.addClassFilter(classType);
                debugProcess.getRequestsManager().enableRequest(exitRequest);
            }
        }
        catch (Exception e) {
            LOG.debug(e);
        }
    }

    @Override
    public String getEventMessage(@NotNull LocatableEvent event) {
        return getEventMessage(event, getFileName());
    }

    private static String getEventMessage(@NotNull LocatableEvent event, @NotNull String defaultFileName) {
        Location location = event.location();
        if (event instanceof MethodEntryEvent) {
            return getEventMessage(true, ((MethodEntryEvent)event).method(), location, defaultFileName);
        }
        if (event instanceof MethodExitEvent) {
            return getEventMessage(false, ((MethodExitEvent)event).method(), location, defaultFileName);
        }
        Object entryProperty = event.request().getProperty(METHOD_ENTRY_KEY);
        if (entryProperty instanceof Boolean) {
            return getEventMessage((Boolean)entryProperty, location.method(), location, defaultFileName);
        }
        return "";
    }

    private static String getEventMessage(boolean entry, Method method, Location location, String defaultFileName) {
        String locationQName = DebuggerUtilsEx.getLocationMethodQName(location);
        String locationFileName = DebuggerUtilsEx.getSourceName(location, e -> defaultFileName);
        int locationLine = location.lineNumber();
        return DebuggerBundle.message(entry ? "status.method.entry.breakpoint.reached" : "status.method.exit.breakpoint.reached",
                                      method.declaringType().name() + "." + method.name() + "()",
                                      locationQName,
                                      locationFileName,
                                      locationLine
        );
    }

    @Override
    public PsiElement getEvaluationElement() {
        return getPsiClass();
    }

    @Override
    protected Icon getDisabledIcon(boolean isMuted) {
        if (DebuggerManagerEx.getInstanceEx(myProject).getBreakpointManager().findMasterBreakpoint(this) != null && isMuted) {
            return AllIcons.Debugger.Db_muted_dep_method_breakpoint;
        }
        return null;
    }

    @Override
    protected Icon getVerifiedIcon(boolean isMuted) {
        return isSuspend() ? AllIcons.Debugger.Db_verified_method_breakpoint : AllIcons.Debugger.Db_verified_no_suspend_method_breakpoint;
    }

    @Override
    @NotNull
    protected Icon getVerifiedWarningsIcon(boolean isMuted) {
        return new LayeredIcon(isMuted ? AllIcons.Debugger.Db_muted_method_breakpoint : AllIcons.Debugger.Db_method_breakpoint,
                               AllIcons.General.WarningDecorator);
    }

    @Override
    public String getDisplayName() {
        StringBuilder buffer = new StringBuilder();
        if(isValid()) {
            String className = getClassName();
            boolean classNameExists = className != null && className.length() > 0;
            if (classNameExists) {
                buffer.append(className);
            }
            if(getMethodName() != null) {
                if (classNameExists) {
                    buffer.append(".");
                }
                buffer.append(getMethodName());
            }
        }
        else {
            buffer.append(DebuggerBundle.message("status.breakpoint.invalid"));
        }
        return buffer.toString();
    }

    @Override
    public boolean evaluateCondition(@NotNull EvaluationContextImpl context, @NotNull LocatableEvent event) throws EvaluateException {
        if (!matchesEvent(event, context.getDebugProcess())) {
            return false;
        }
        return super.evaluateCondition(context, event);
    }

    private boolean matchesEvent(@NotNull LocatableEvent event, DebugProcessImpl process) throws EvaluateException {
        if (isEmulated()) {
            return true;
        }
        if (getMethodName() == null || mySignature == null) {
            return false;
        }
        Method method = event.location().method();
        return method != null && method.name().equals(getMethodName()) && method.signature().equals(mySignature.getName(process));
    }

    @Nullable
    public static KotlinFunctionBreakpoint create(@NotNull Project project, XBreakpoint xBreakpoint) {
        KotlinFunctionBreakpoint breakpoint = new KotlinFunctionBreakpoint(project, xBreakpoint);
        return (KotlinFunctionBreakpoint)breakpoint.init();
    }

    //public boolean canMoveTo(final SourcePosition position) {
    //  return super.canMoveTo(position) && PositionUtil.getPsiElementAt(getProject(), PsiMethod.class, position) != null;
    //}

    /**
     * finds FQ method's class name and method's signature
     */
    @Nullable
    private static MethodDescriptor getMethodDescriptor(@NotNull Project project, @NotNull SourcePosition sourcePosition) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(sourcePosition.getFile());
        if (document == null) {
            return null;
        }
        //final int endOffset = document.getLineEndOffset(sourcePosition);
        //final MethodDescriptor descriptor = docManager.commitAndRunReadAction(new Computable<MethodDescriptor>() {
        // conflicts with readAction on initial breakpoints creation
        MethodDescriptor descriptor = ReadAction.compute(() -> {
            // MODIFICATION: Start Kotlin implementation
            PsiMethod method = resolveJvmMethodFromKotlinDeclaration(project, sourcePosition);
            // MODIFICATION: End Kotlin implementation
            if (method == null) {
                return null;
            }
            int methodOffset = method.getTextOffset();
            if (!DocumentUtil.isValidOffset(methodOffset, document) || document.getLineNumber(methodOffset) < sourcePosition.getLine()) {
                return null;
            }

            PsiIdentifier identifier = method.getNameIdentifier();
            int methodNameOffset = identifier != null? identifier.getTextOffset() : methodOffset;
            MethodDescriptor res =
                    new MethodDescriptor();
            res.methodName = JVMNameUtil.getJVMMethodName(method);
            try {
                res.methodSignature = JVMNameUtil.getJVMSignature(method);
                res.isStatic = method.hasModifierProperty(PsiModifier.STATIC);
            }
            catch (IndexNotReadyException ignored) {
                return null;
            }
            res.methodLine = document.getLineNumber(methodNameOffset);
            return res;
        });
        if (descriptor == null || descriptor.methodName == null || descriptor.methodSignature == null) {
            return null;
        }
        return descriptor;
    }

    // MODIFICATION: Start Kotlin implementation
    @Nullable
    private static PsiMethod resolveJvmMethodFromKotlinDeclaration(@NotNull Project project, @NotNull SourcePosition sourcePosition) {
        KtDeclaration declaration = PositionUtil.getPsiElementAt(project, KtDeclaration.class, sourcePosition);

        if (declaration instanceof KtClass) {
            KtPrimaryConstructor constructor = ((KtClass) declaration).getPrimaryConstructor();
            if (constructor != null) {
                declaration = constructor;
            } else {
                KtLightClass lightClass = LightClassUtilsKt.toLightClass((KtClassOrObject) declaration);
                if (lightClass != null) {
                    PsiMethod[] constructors = lightClass.getConstructors();
                    if (constructors.length > 0) {
                        return constructors[0];
                    }
                }

                return null;
            }
        }

        if (declaration == null) {
            return null;
        }

        for (PsiElement element : LightClassUtilsKt.toLightElements(declaration)) {
            if (element instanceof PsiMethod) {
                // TODO handle all light methods
                return (PsiMethod) element;
            }
        }

        return null;
    }
    // MODIFICATION: End Kotlin implementation

    @Nullable
    private static <T extends EventRequest> T findRequest(
            @NotNull DebugProcessImpl debugProcess,
            Class<T> requestClass,
            Requestor requestor
    ) {
        return StreamEx.of(debugProcess.getRequestsManager().findRequests(requestor)).select(requestClass).findFirst().orElse(null);
    }

    @Override
    public void readExternal(@NotNull Element breakpointNode) throws InvalidDataException {
        super.readExternal(breakpointNode);
        try {
            getProperties().WATCH_ENTRY = Boolean.valueOf(JDOMExternalizerUtil.readField(breakpointNode, "WATCH_ENTRY"));
        } catch (Exception ignored) {
        }
        try {
            getProperties().WATCH_EXIT = Boolean.valueOf(JDOMExternalizerUtil.readField(breakpointNode, "WATCH_EXIT"));
        } catch (Exception ignored) {
        }
    }

    private boolean isEmulated() {
        return getProperties().EMULATED;
    }

    @Override
    public boolean isWatchEntry() {
        return getProperties().WATCH_ENTRY;
    }

    @Override
    public boolean isWatchExit() {
        return getProperties().WATCH_EXIT;
    }

    @Override
    public StreamEx matchingMethods(StreamEx<Method> methods, DebugProcessImpl debugProcess) {
        try {
            String methodName = getMethodName();
            String signature = mySignature != null ? mySignature.getName(debugProcess) : null;
            return methods.filter(m -> Comparing.equal(methodName, m.name()) && Comparing.equal(signature, m.signature())).limit(1);
        }
        catch (EvaluateException e) {
            LOG.warn(e);
        }
        return StreamEx.empty();
    }

    @Nullable
    private String getMethodName() {
        return getProperties().myMethodName;
    }

    private void setMethodName(@Nullable String methodName) {
        getProperties().myMethodName = methodName;
    }

    private static final class MethodDescriptor {
        String methodName;
        JVMName methodSignature;
        boolean isStatic;
        int methodLine;
    }

    private static void processPreparedSubTypes(ReferenceType classType,
            BiConsumer<ReferenceType, ClassesByNameProvider> consumer,
            ProgressIndicator progressIndicator) {
        long start = 0;
        if (LOG.isDebugEnabled()) {
            start = System.currentTimeMillis();
        }
        progressIndicator.setIndeterminate(false);
        progressIndicator.start();
        progressIndicator.setText(DebuggerBundle.message("label.method.breakpoints.processing.classes"));
        try {
            MultiMap<ReferenceType, ReferenceType> inheritance = new MultiMap<>();
            List<ReferenceType> allTypes = classType.virtualMachine().allClasses();
            for (int i = 0; i < allTypes.size(); i++) {
                if (progressIndicator.isCanceled()) {
                    return;
                }
                ReferenceType type = allTypes.get(i);
                if (type.isPrepared()) {
                    try {
                        supertypes(type).forEach(st -> inheritance.putValue(st, type));
                    }
                    catch (ObjectCollectedException ignored) {
                    }
                }
                progressIndicator.setText2(i + "/" + allTypes.size());
                progressIndicator.setFraction((double)i / allTypes.size());
            }
            List<ReferenceType> types = StreamEx.ofTree(classType, t -> StreamEx.of(inheritance.get(t))).skip(1).toList();

            progressIndicator.setText(DebuggerBundle.message("label.method.breakpoints.setting.breakpoints"));

            ClassesByNameProvider classesByName = ClassesByNameProvider.createCache(allTypes);

            for (int i = 0; i < types.size(); i++) {
                if (progressIndicator.isCanceled()) {
                    return;
                }
                consumer.accept(types.get(i), classesByName);

                progressIndicator.setText2(i + "/" + types.size());
                progressIndicator.setFraction((double)i / types.size());
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Processed " + types.size() + " classes in " + (System.currentTimeMillis() - start) + "ms");
            }
        }
        finally {
            progressIndicator.stop();
        }
    }

    // MODIFICATION: Add utilities absent in older platform versions
    private static boolean instanceOf(@Nullable ReferenceType type, @NotNull ReferenceType superType) {
        if (type == null) {
            return false;
        }
        if (superType.equals(type)) {
            return true;
        }
        return supertypes(type).anyMatch(t -> instanceOf(t, superType));
    }

    private static Stream<? extends ReferenceType> supertypes(ReferenceType type) {
        if (type instanceof InterfaceType) {
            return ((InterfaceType)type).superinterfaces().stream();
        } else if (type instanceof ClassType) {
            return StreamEx.<ReferenceType>ofNullable(((ClassType)type).superclass()).prepend(((ClassType)type).interfaces());
        }
        return StreamEx.empty();
    }
    // MODIFICATION: End
}
