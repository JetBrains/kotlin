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
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import kotlin.IntRange;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetFunction;
import org.jetbrains.kotlin.psi.JetFunctionLiteral;
import org.jetbrains.kotlin.psi.JetNamedFunction;

import java.util.List;

public class DebuggerSteppingHelper {

    public static DebugProcessImpl.ResumeCommand createStepOverCommand(
            final SuspendContextImpl suspendContext,
            final boolean ignoreBreakpoints,
            final JetFile file,
            final IntRange linesRange,
            final List<JetFunction> inlineArguments,
            final List<PsiElement> additionalElementsToSkip
    ) {
        final DebugProcessImpl debugProcess = suspendContext.getDebugProcess();
        return debugProcess.new ResumeCommand(suspendContext) {
            @Override
            public void contextAction() {
                final StackFrameProxyImpl frameProxy = suspendContext.getFrameProxy();
                if (frameProxy != null) {
                    DebugProcessImpl.ResumeCommand runToCursorCommand = ApplicationManager.getApplication().runReadAction(new Computable<DebugProcessImpl.ResumeCommand>() {
                        @Override
                        public DebugProcessImpl.ResumeCommand compute() {
                            try {
                                XSourcePositionImpl position = KotlinSteppingCommandProviderKt.getStepOutPosition(
                                        frameProxy.location(),
                                        file,
                                        linesRange,
                                        inlineArguments,
                                        additionalElementsToSkip
                                );
                                if (position != null) {
                                    return debugProcess.createRunToCursorCommand(suspendContext, position, ignoreBreakpoints);
                                }
                            }
                            catch (EvaluateException ignored) {
                            }
                            return null;
                        }
                    });

                    if (runToCursorCommand != null) {
                        runToCursorCommand.contextAction();
                        return;
                    }
                }

                debugProcess.createStepOutCommand(suspendContext).contextAction();
            }
        };
    }

    public static DebugProcessImpl.ResumeCommand createStepOutCommand(
            final SuspendContextImpl suspendContext,
            final boolean ignoreBreakpoints,
            final List<JetNamedFunction> inlineFunctions,
            final JetFunctionLiteral inlineArgument
    ) {
        final DebugProcessImpl debugProcess = suspendContext.getDebugProcess();
        return debugProcess.new ResumeCommand(suspendContext) {
            @Override
            public void contextAction() {
                final StackFrameProxyImpl frameProxy = suspendContext.getFrameProxy();
                if (frameProxy != null) {
                    DebugProcessImpl.ResumeCommand runToCursorCommand = ApplicationManager.getApplication().runReadAction(new Computable<DebugProcessImpl.ResumeCommand>() {
                        @Override
                        public DebugProcessImpl.ResumeCommand compute() {
                            try {
                                XSourcePositionImpl position = KotlinSteppingCommandProviderKt.getStepOverPosition(
                                        frameProxy.location(),
                                        suspendContext,
                                        inlineFunctions,
                                        inlineArgument
                                );
                                if (position != null) {
                                    return debugProcess.createRunToCursorCommand(suspendContext, position, ignoreBreakpoints);
                                }
                            }
                            catch (EvaluateException ignored) {
                            }
                            return null;
                        }
                    });

                    if (runToCursorCommand != null) {
                        runToCursorCommand.contextAction();
                        return;
                    }
                }

                debugProcess.createStepOverCommand(suspendContext, ignoreBreakpoints).contextAction();
            }
        };
    }
}
