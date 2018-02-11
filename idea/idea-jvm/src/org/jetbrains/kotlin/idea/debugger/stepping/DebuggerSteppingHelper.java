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

package org.jetbrains.kotlin.idea.debugger.stepping;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.ui.classFilter.DebuggerClassFilterProvider;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.debugger.NoStrataPositionManagerHelperKt;
import org.jetbrains.kotlin.psi.KtFunctionLiteral;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.util.ArrayList;
import java.util.List;


public class DebuggerSteppingHelper {

    public static DebugProcessImpl.ResumeCommand createStepOverCommand(
            final SuspendContextImpl suspendContext,
            final boolean ignoreBreakpoints,
            final KotlinSteppingCommandProvider.KotlinSourcePosition kotlinSourcePosition
    ) {
        final DebugProcessImpl debugProcess = suspendContext.getDebugProcess();

        return debugProcess.new ResumeCommand(suspendContext) {
            @Override
            public void contextAction() {
                boolean isDexDebug = NoStrataPositionManagerHelperKt.isDexDebug(suspendContext.getDebugProcess());

                try {
                    StackFrameProxyImpl frameProxy = suspendContext.getFrameProxy();
                    if (frameProxy != null) {
                        Action action = KotlinSteppingCommandProviderKt.getStepOverAction(
                                frameProxy.location(),
                                kotlinSourcePosition,
                                frameProxy,
                                isDexDebug
                        );

                        createStepRequest(
                                suspendContext, getContextThread(),
                                debugProcess.getVirtualMachineProxy().eventRequestManager(),
                                StepRequest.STEP_LINE, StepRequest.STEP_OUT);

                        action.apply(debugProcess, suspendContext, ignoreBreakpoints);
                        return;
                    }

                    debugProcess.createStepOutCommand(suspendContext).contextAction();
                }
                catch (EvaluateException ignored) {
                }
            }
        };
    }

    public static DebugProcessImpl.ResumeCommand createStepOutCommand(
            final SuspendContextImpl suspendContext,
            final boolean ignoreBreakpoints,
            final List<KtNamedFunction> inlineFunctions,
            final KtFunctionLiteral inlineArgument
    ) {
        final DebugProcessImpl debugProcess = suspendContext.getDebugProcess();
        return debugProcess.new ResumeCommand(suspendContext) {
            @Override
            public void contextAction() {
                try {
                    StackFrameProxyImpl frameProxy = suspendContext.getFrameProxy();
                    if (frameProxy != null) {
                        Action action = KotlinSteppingCommandProviderKt.getStepOutAction(
                                frameProxy.location(),
                                suspendContext,
                                inlineFunctions,
                                inlineArgument
                        );

                        createStepRequest(
                                suspendContext, getContextThread(),
                                debugProcess.getVirtualMachineProxy().eventRequestManager(),
                                StepRequest.STEP_LINE, StepRequest.STEP_OUT);

                        action.apply(debugProcess, suspendContext, ignoreBreakpoints);
                        return;
                    }

                    debugProcess.createStepOverCommand(suspendContext, ignoreBreakpoints).contextAction();
                }
                catch (EvaluateException ignored) {
                }
            }
        };
    }

    // copied from DebugProcessImpl.doStep
    private static void createStepRequest(
            @NotNull SuspendContextImpl suspendContext,
            @Nullable ThreadReferenceProxyImpl stepThread,
            @NotNull EventRequestManager requestManager,
            int size, int depth
    ) {
        if (stepThread == null) {
            return;
        }
        try {
            ThreadReference stepThreadReference = stepThread.getThreadReference();

            requestManager.deleteEventRequests(requestManager.stepRequests());

            StepRequest stepRequest = requestManager.createStepRequest(stepThreadReference, size, depth);

            List<ClassFilter> activeFilters = getActiveFilters();

            if (!activeFilters.isEmpty()) {
                String currentClassName = getCurrentClassName(stepThread);
                if (currentClassName == null || !DebuggerUtilsEx.isFiltered(currentClassName, activeFilters)) {
                    // add class filters
                    for (ClassFilter filter : activeFilters) {
                        stepRequest.addClassExclusionFilter(filter.getPattern());
                    }
                }
            }

            // suspend policy to match the suspend policy of the context:
            // if all threads were suspended, then during stepping all the threads must be suspended
            // if only event thread were suspended, then only this particular thread must be suspended during stepping
            stepRequest.setSuspendPolicy(suspendContext.getSuspendPolicy() == EventRequest.SUSPEND_EVENT_THREAD
                                         ? EventRequest.SUSPEND_EVENT_THREAD
                                         : EventRequest.SUSPEND_ALL);

            stepRequest.enable();
        }
        catch (ObjectCollectedException ignored) {

        }
    }

    // copied from DebugProcessImpl.getActiveFilters
    @NotNull
    private static List<ClassFilter> getActiveFilters() {
        List<ClassFilter> activeFilters = new ArrayList<ClassFilter>();
        DebuggerSettings settings = DebuggerSettings.getInstance();
        if (settings.TRACING_FILTERS_ENABLED) {
            for (ClassFilter filter : settings.getSteppingFilters()) {
                if (filter.isEnabled()) {
                    activeFilters.add(filter);
                }
            }
        }
        for (DebuggerClassFilterProvider provider : Extensions.getExtensions(DebuggerClassFilterProvider.EP_NAME)) {
            for (ClassFilter filter : provider.getFilters()) {
                if (filter.isEnabled()) {
                    activeFilters.add(filter);
                }
            }
        }
        return activeFilters;
    }

    // copied from DebugProcessImpl.getActiveFilters
    @Nullable
    private static String getCurrentClassName(ThreadReferenceProxyImpl thread) {
        try {
            if (thread != null && thread.frameCount() > 0) {
                StackFrameProxyImpl stackFrame = thread.frame(0);
                if (stackFrame != null) {
                    Location location = stackFrame.location();
                    ReferenceType referenceType = location == null ? null : location.declaringType();
                    if (referenceType != null) {
                        return referenceType.name();
                    }
                }
            }
        }
        catch (EvaluateException ignored) {
        }
        return null;
    }
}
