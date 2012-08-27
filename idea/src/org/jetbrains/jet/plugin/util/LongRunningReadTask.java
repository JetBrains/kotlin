/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
* @author Nikolay Krasko
*/
public abstract class LongRunningReadTask<RequestInfo, ResultData> {
    enum State {
        NOT_INITIALIZED,
        INITIALIZED,
        STARTED,
        FINISHED
    }

    private ProgressIndicator progressIndicator = null;
    private RequestInfo requestInfo = null;
    private State currentState = State.NOT_INITIALIZED;

    protected LongRunningReadTask() {}

    /** Should be executed in GUI thread */
    public boolean isShouldStartWithCancel(@Nullable LongRunningReadTask<RequestInfo, ResultData> previousTask) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (currentState != State.INITIALIZED) {
            throw new IllegalStateException("Task should be initialized state. Call init() method.");
        }

        if (requestInfo == null) {
            return false;
        }

        if (previousTask != null && previousTask.currentState == State.STARTED) {
            if (!requestInfo.equals(previousTask.requestInfo)) {
                // Previous task counting data for outdated request - cancel it.
                previousTask.progressIndicator.cancel();
                return true;
            }
            else {
                // If previous task is in progress and counting result for similar result don't start new task
                return false;
            }
        }

        return true;
    }

    /** Should be executed in GUI thread */
    public final void run() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (currentState != State.INITIALIZED) {
            throw new IllegalStateException("Task should be initialized with init() method");
        }

        currentState = State.STARTED;

        beforeRun();

        progressIndicator = new ProgressIndicatorBase();

        final RequestInfo requestInfoCopy = cloneRequestInfo(requestInfo);

        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                runWithWriteActionPriority(progressIndicator, new Runnable() {
                    @Override
                    public void run() {
                        ResultData resultData = null;
                        try {
                            resultData = processRequest(requestInfoCopy);
                        }
                        finally {
                            // Back to GUI thread for submitting result
                            final ResultData finalResult = resultData;
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    resultReady(finalResult);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    public final void init() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        requestInfo = prepareRequestInfo();
        currentState = State.INITIALIZED;
    }

    private void resultReady(ResultData resultData) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        currentState = State.FINISHED;
        onResultReady(requestInfo, resultData);
    }

    /**
     * This method should prepare a copy of request object that will be used during the processing of the
     * request in thread pool. If RequestInfo class is thread safe this method can return
     * a reference to already constructed object.
     *
     * By default this method will reconstruct request object with prepareRequestInfo method.
     *
     * Executed in GUI Thread.
     */
    @SuppressWarnings("UnusedParameters")
    @NotNull
    protected RequestInfo cloneRequestInfo(@NotNull RequestInfo requestInfo) {
        RequestInfo cloneRequestInfo = prepareRequestInfo();
        if (cloneRequestInfo == null) {
            throw new IllegalStateException("Cloned request object can't be null");
        }

        return cloneRequestInfo;
    }

    /**
     * Executed in GUI Thread.
     */
    @Nullable
    protected abstract RequestInfo prepareRequestInfo();

    /**
     * Executed in GUI Thread right before task run. Do nothing by default.
     */
    protected void beforeRun() {}

    /**
     * Executed in thread pool under read lock with write priority.
     */
    @Nullable
    protected abstract ResultData processRequest(@NotNull RequestInfo requestInfo);

    /**
     * Executed in GUI Thread. Do nothing by default.
     */
    protected void onResultReady(@NotNull RequestInfo requestInfo, @Nullable ResultData resultData) {}

    /**
     * Execute action with immediate stop when write lock is required.
     *
     * {@link ProgressIndicatorUtils#runWithWriteActionPriority(Runnable)}
     *
     * @param indicator
     * @param action
     */
    public static void runWithWriteActionPriority(@NotNull final ProgressIndicator indicator, @NotNull final Runnable action) {
        final ApplicationAdapter listener = new ApplicationAdapter() {
            @Override
            public void beforeWriteActionStart(Object action) {
                indicator.cancel();
            }
        };
        final Application application = ApplicationManager.getApplication();
        try {
            application.addApplicationListener(listener);
            ProgressManager.getInstance().runProcess(new Runnable() {
                @Override
                public void run() {
                    application.runReadAction(action);
                }
            }, indicator);
        }
        finally {
            application.removeApplicationListener(listener);
        }
    }
}
