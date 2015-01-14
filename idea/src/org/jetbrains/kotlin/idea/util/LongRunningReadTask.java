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

package org.jetbrains.kotlin.idea.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LongRunningReadTask<RequestInfo, ResultData> {
    enum State {
        NOT_INITIALIZED,
        INITIALIZED,
        STARTED,
        FINISHED,
        FINISHED_WITH_ACTUAL_DATA
    }

    private ProgressIndicator progressIndicator = null;
    private RequestInfo requestInfo = null;
    private State currentState = State.NOT_INITIALIZED;

    protected LongRunningReadTask() {}

    /** Should be executed in GUI thread */
    public boolean shouldStart(@Nullable LongRunningReadTask<RequestInfo, ResultData> previousTask) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (currentState != State.INITIALIZED) {
            throw new IllegalStateException("Task should be initialized state. Call init() method.");
        }

        // Cancel previous task if necessary
        if (previousTask != null && previousTask.currentState == State.STARTED) {
            if (requestInfo == null || !requestInfo.equals(previousTask.requestInfo)) {
                previousTask.progressIndicator.cancel();
            }
        }

        if (requestInfo == null) {
            if (previousTask != null && (previousTask.currentState == State.FINISHED_WITH_ACTUAL_DATA || previousTask.currentState == State.FINISHED)) {
                previousTask.hideResultOnInvalidLocation();
            }

            return false;
        }

        if (previousTask != null) {
            if (previousTask.currentState == State.STARTED) {
                // Start new task only if previous isn't working on similar request
                return !requestInfo.equals(previousTask.requestInfo);
            }
            else if (previousTask.currentState == State.FINISHED_WITH_ACTUAL_DATA) {
                if (requestInfo.equals(previousTask.requestInfo)) {
                    return false;
                }
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

        if (requestInfo == null) {
            throw new IllegalStateException("Invalid request for task beginning");
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

    /**
     * Executed in GUI Thread.
     *
     * @return true if new request was successfully created, false if request is invalid and shouldn't be started
     */
    public final boolean init() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        requestInfo = prepareRequestInfo();
        currentState = State.INITIALIZED;

        return requestInfo != null;
    }

    private void resultReady(ResultData resultData) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        currentState = State.FINISHED;

        if (resultData != null) {
            RequestInfo actualInfo = prepareRequestInfo();
            if (requestInfo.equals(actualInfo)) {
                currentState = State.FINISHED_WITH_ACTUAL_DATA;
                onResultReady(actualInfo, resultData);
            }
        }
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
     *
     * @return null if current request is invalid and task shouldn't be executed.
     */
    @Nullable
    protected abstract RequestInfo prepareRequestInfo();

    /**
     * Executed in GUI Thread.
     */
    protected void hideResultOnInvalidLocation() {}

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
        ApplicationAdapter listener = new ApplicationAdapter() {
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
