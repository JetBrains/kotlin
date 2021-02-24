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

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.RequestHint;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.ui.classFilter.DebuggerClassFilterProvider;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.debugger.DebuggerUtilKt;
import org.jetbrains.kotlin.idea.debugger.SafeUtilKt;

import java.util.List;

public class DebuggerSteppingHelper {
    public static DebugProcessImpl.ResumeCommand createStepOverCommand(
            SuspendContextImpl suspendContext,
            boolean ignoreBreakpoints,
            SourcePosition sourcePosition
    ) {
        DebugProcessImpl debugProcess = suspendContext.getDebugProcess();

        return debugProcess.new ResumeCommand(suspendContext) {
            @Override
            public void contextAction() {
                StackFrameProxyImpl frameProxy = suspendContext.getFrameProxy();
                Location location = frameProxy == null ? null : SafeUtilKt.safeLocation(frameProxy);

                if (location != null) {
                    KotlinStepAction action = KotlinSteppingCommandProviderKt
                                .getStepOverAction(location, sourcePosition, suspendContext, frameProxy);

                    createStepRequest(
                            suspendContext, getContextThread(),
                            debugProcess.getVirtualMachineProxy().eventRequestManager(),
                            StepRequest.STEP_LINE, StepRequest.STEP_OUT);

                    action.apply(debugProcess, suspendContext, ignoreBreakpoints);
                    return;
                }

                debugProcess.createStepOutCommand(suspendContext).contextAction();
            }
        };
    }

    public static DebugProcessImpl.ResumeCommand createStepOverCommandForSuspendSwitch(SuspendContextImpl suspendContext) {
        DebugProcessImpl debugProcess = suspendContext.getDebugProcess();
        return debugProcess.new StepOverCommand(suspendContext, false, null, StepRequest.STEP_MIN) {
            @NotNull @Override
            protected RequestHint getHint(SuspendContextImpl suspendContext, ThreadReferenceProxyImpl stepThread) {
                RequestHint hint = new RequestHint(stepThread, suspendContext, StepRequest.STEP_MIN, StepRequest.STEP_OVER, myMethodFilter) {
                    @Override
                    public int getNextStepDepth(SuspendContextImpl context) {
                        StackFrameProxyImpl frameProxy = context.getFrameProxy();
                        if (frameProxy != null && DebuggerUtilKt.isOnSuspensionPoint(frameProxy)) {
                            return StepRequest.STEP_OVER;
                        }

                        return super.getNextStepDepth(context);
                    }
                };
                hint.setIgnoreFilters(suspendContext.getDebugProcess().getSession().shouldIgnoreSteppingFilters());
                return hint;
            }
        };
    }

    public static DebugProcessImpl.ResumeCommand createStepOutCommand(SuspendContextImpl suspendContext, boolean ignoreBreakpoints) {
        DebugProcessImpl debugProcess = suspendContext.getDebugProcess();
        return debugProcess.new ResumeCommand(suspendContext) {
            @Override
            public void contextAction() {
                StackFrameProxyImpl frameProxy = suspendContext.getFrameProxy();
                Location location = frameProxy == null ? null : SafeUtilKt.safeLocation(frameProxy);

                if (location != null) {
                    KotlinStepAction action = KotlinSteppingCommandProviderKt.getStepOutAction(location, frameProxy);

                    createStepRequest(
                            suspendContext, getContextThread(),
                            debugProcess.getVirtualMachineProxy().eventRequestManager(),
                            StepRequest.STEP_LINE, StepRequest.STEP_OUT);

                    action.apply(debugProcess, suspendContext, ignoreBreakpoints);
                    return;
                }

                debugProcess.createStepOverCommand(suspendContext, ignoreBreakpoints).contextAction();
            }
        };
    }

    // copied from DebugProcessImpl.doStep
    private static void createStepRequest(
            @NotNull SuspendContextImpl suspendContext,
            @Nullable ThreadReferenceProxyImpl stepThread,
            @NotNull EventRequestManager requestManager,
            @SuppressWarnings("SameParameterValue") int size,
            @SuppressWarnings("SameParameterValue") int depth
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
        DebuggerSettings settings = DebuggerSettings.getInstance();
        StreamEx<ClassFilter> stream = StreamEx.of(DebuggerClassFilterProvider.EP_NAME.getExtensionList())
                .flatCollection(DebuggerClassFilterProvider::getFilters);
        if (settings.TRACING_FILTERS_ENABLED) {
            stream = stream.prepend(settings.getSteppingFilters());
        }
        return stream.filter(ClassFilter::isEnabled).toList();
    }

    // copied from DebugProcessImpl.getCurrentClassName
    @Nullable
    private static String getCurrentClassName(ThreadReferenceProxyImpl thread) {
        try {
            if (thread != null && thread.frameCount() > 0) {
                StackFrameProxyImpl stackFrame = thread.frame(0);
                Location location = stackFrame == null ? null : SafeUtilKt.safeLocation(stackFrame);

                if (stackFrame != null) {
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
