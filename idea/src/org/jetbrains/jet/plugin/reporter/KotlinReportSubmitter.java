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

package org.jetbrains.jet.plugin.reporter;

import com.intellij.CommonBundle;
import com.intellij.diagnostic.*;
import com.intellij.errorreport.ErrorReportSender;
import com.intellij.errorreport.bean.ErrorBean;
import com.intellij.errorreport.error.InternalEAPException;
import com.intellij.errorreport.error.NoSuchEAPUserException;
import com.intellij.errorreport.error.UpdateAvailableException;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.idea.IdeaLogger;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * We need to wrap ITNReporter into this delegating class to work around the following problem:
 *
 * Kotlin's lifecycle does not align with the one of IDEA, so every now and then we are in the situation when users
 * install an unstable build of Kotlin on a released build of IDEA. Since IDEA does not show exceptions from ITNReporter
 * in release build, the user doesn't see exceptions from Kotlin in such a situations. Wrapping solves this problem:
 * even release builds of IDEA will report Kotlin exceptions to the user.
 *
 * @author abreslav
 */

// TODO: Extend ITNReporter and override showErrorInRelease when patch is ready
// Private methods of ITNReporter were copied here as we also want allow submitting to JetBrains same way the ITNReporter does it.
public class KotlinReportSubmitter extends ErrorReportSubmitter {
    private static int previousExceptionThreadId = 0;
    private static boolean wasException = false;
    @NonNls private static final String URL_HEADER = "http://www.intellij.net/tracker/idea/viewSCR?publicId=";

    public String getReportActionText() {
        return DiagnosticBundle.message("error.report.to.jetbrains.action");
    }

    public SubmittedReportInfo submit(IdeaLoggingEvent[] events, Component parentComponent) {
        // obsolete API
        return new SubmittedReportInfo(null, "0", SubmittedReportInfo.SubmissionStatus.FAILED);
    }

    @Override
    public boolean trySubmitAsync(IdeaLoggingEvent[] events,
            String additionalInfo,
            Component parentComponent,
            Consumer<SubmittedReportInfo> consumer) {
        return sendError(events[0], additionalInfo, parentComponent, consumer);
    }

    /**
     * @noinspection ThrowablePrintStackTrace
     */
    private static boolean sendError(IdeaLoggingEvent event,
            String additionalInfo,
            final Component parentComponent,
            final Consumer<SubmittedReportInfo> callback) {
        ErrorBean errorBean = new ErrorBean(event.getThrowable(), IdeaLogger.ourLastActionId);

        return doSubmit(event, parentComponent, callback, errorBean, additionalInfo);
    }

    private static boolean doSubmit(final IdeaLoggingEvent event,
            final Component parentComponent,
            final Consumer<SubmittedReportInfo> callback,
            final ErrorBean errorBean,
            final String description) {
        final DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);
        final Project project = PlatformDataKeys.PROJECT.getData(dataContext);

        final ErrorReportConfigurable errorReportConfigurable = ErrorReportConfigurable.getInstance();
        if (!errorReportConfigurable.KEEP_ITN_PASSWORD &&
            !StringUtil.isEmpty(errorReportConfigurable.ITN_LOGIN) &&
            StringUtil.isEmpty(errorReportConfigurable.getPlainItnPassword())) {
            final JetBrainsAccountDialog dlg = new JetBrainsAccountDialog(parentComponent);
            dlg.show();
            if (!dlg.isOK()) {
                return false;
            }
        }

        errorBean.setDescription(description);
        errorBean.setMessage(event.getMessage());

        if (previousExceptionThreadId != 0) {
            errorBean.setPreviousException(previousExceptionThreadId);
        }

        Throwable t = event.getThrowable();
        if (t != null) {
            final PluginId pluginId = IdeErrorsDialog.findPluginId(t);
            if (pluginId != null) {
                final IdeaPluginDescriptor ideaPluginDescriptor = PluginManager.getPlugin(pluginId);
                if (ideaPluginDescriptor != null && !ideaPluginDescriptor.isBundled()) {
                    errorBean.setPluginName(ideaPluginDescriptor.getName());
                    errorBean.setPluginVersion(ideaPluginDescriptor.getVersion());
                }
            }
        }

        Object data = event.getData();

        if (data instanceof AbstractMessage) {
            errorBean.setAssigneeId(((AbstractMessage)data).getAssigneeId());
        }

        if (data instanceof LogMessageEx) {
            errorBean.setAttachments(((LogMessageEx)data).getAttachments());
        }

        @NonNls String login = errorReportConfigurable.ITN_LOGIN;
        @NonNls String password = errorReportConfigurable.getPlainItnPassword();
        if (login.trim().length() == 0 && password.trim().length() == 0) {
            login = "idea_anonymous";
            password = "guest";
        }

        ErrorReportSender.sendError(project, login, password, errorBean, new Consumer<Integer>() {
                                        @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
                                        @Override
                                        public void consume(Integer threadId) {
                                            previousExceptionThreadId = threadId;
                                            wasException = true;
                                            final SubmittedReportInfo reportInfo =
                                                    new SubmittedReportInfo(URL_HEADER + threadId, String.valueOf(threadId),
                                                                            SubmittedReportInfo.SubmissionStatus.NEW_ISSUE);
                                            callback.consume(reportInfo);
                                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    StringBuilder text = new StringBuilder("<html>");
                                                    final String url = IdeErrorsDialog.getUrl(reportInfo, true);
                                                    IdeErrorsDialog.appendSubmissionInformation(reportInfo, text, url);
                                                    text.append(".");
                                                    if (reportInfo.getStatus() != SubmittedReportInfo.SubmissionStatus.FAILED) {
                                                        text.append("<br/>").append(DiagnosticBundle.message("error.report.gratitude"));
                                                    }
                                                    text.append("</html>");
                                                    NotificationType type =
                                                            reportInfo.getStatus() == SubmittedReportInfo.SubmissionStatus.FAILED
                                                            ? NotificationType.ERROR
                                                            : NotificationType.INFORMATION;
                                                    NotificationListener listener = url != null ? new NotificationListener() {
                                                        @Override
                                                        public void hyperlinkUpdate(@NotNull Notification notification,
                                                                @NotNull HyperlinkEvent event) {
                                                            BrowserUtil.launchBrowser(url);
                                                            notification.expire();
                                                        }
                                                    } : null;
                                                    ReportMessages.GROUP.createNotification(ReportMessages.ERROR_REPORT,
                                                                                            text.toString(),
                                                                                            type, listener).setImportant(false).notify(project);
                                                }
                                            });
                                        }
                                    }, new Consumer<Exception>() {
                                        @Override
                                        public void consume(final Exception e) {
                                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    String msg;
                                                    if (e instanceof NoSuchEAPUserException) {
                                                        msg = DiagnosticBundle.message("error.report.authentication.failed");
                                                    }
                                                    else if (e instanceof InternalEAPException) {
                                                        msg = DiagnosticBundle.message("error.report.posting.failed", e.getMessage());
                                                    }
                                                    else {
                                                        msg = DiagnosticBundle.message("error.report.sending.failure");
                                                    }
                                                    if (e instanceof UpdateAvailableException) {
                                                        ApplicationInfoEx appInfo = (ApplicationInfoEx) ApplicationManager.getApplication()
                                                                .getComponent(ApplicationInfo.class);
                                                        String message = DiagnosticBundle.message(
                                                                appInfo.isEAP()
                                                                ? "error.report.new.eap.build.message"
                                                                : "error.report.new.build.message", e.getMessage());
                                                        showMessageDialog(parentComponent, project, message, CommonBundle.getWarningTitle(),
                                                                          Messages.getWarningIcon());
                                                        callback.consume(new SubmittedReportInfo(null, "0",
                                                                                                 SubmittedReportInfo.SubmissionStatus.FAILED));
                                                    }
                                                    else if (showYesNoDialog(parentComponent, project, msg, ReportMessages.ERROR_REPORT,
                                                                             Messages.getErrorIcon()) != 0) {
                                                        callback.consume(new SubmittedReportInfo(null, "0",
                                                                                                 SubmittedReportInfo.SubmissionStatus.FAILED));
                                                    }
                                                    else {
                                                        if (e instanceof NoSuchEAPUserException) {
                                                            final JetBrainsAccountDialog dialog;
                                                            if (parentComponent.isShowing()) {
                                                                dialog = new JetBrainsAccountDialog(parentComponent);
                                                            }
                                                            else {
                                                                dialog = new JetBrainsAccountDialog(project);
                                                            }
                                                            dialog.show();
                                                        }
                                                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                doSubmit(event, parentComponent, callback, errorBean, description);
                                                            }
                                                        });
                                                    }
                                                }
                                            });
                                        }
                                    }
        );
        return true;
    }

    private static void showMessageDialog(Component parentComponent, Project project, String message, String title, Icon icon) {
        if (parentComponent.isShowing()) {
            Messages.showMessageDialog(parentComponent, message, title, icon);
        } else {
            Messages.showMessageDialog(project, message, title, icon);
        }
    }

    private static int showYesNoDialog(Component parentComponent, Project project, String message, String title, Icon icon) {
        if (parentComponent.isShowing()) {
            return Messages.showYesNoDialog(parentComponent, message, title, icon);
        } else {
            return Messages.showYesNoDialog(project, message, title, icon);
        }
    }
}
