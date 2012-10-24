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

package org.jetbrains.jet.plugin.filters;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.execution.filters.ExceptionFilter;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JetExceptionFilter implements Filter {
    @NotNull private final ExceptionFilter exceptionFilter;
    @NotNull private final GlobalSearchScope searchScope;

    public JetExceptionFilter(@NotNull GlobalSearchScope searchScope) {
        exceptionFilter = new ExceptionFilter(searchScope);
        this.searchScope = searchScope;
    }

    @Nullable
    private HyperlinkInfo createHyperlinkInfo(@NotNull String line) {
        Project project = searchScope.getProject();
        if (project == null) return null;

        final StackTraceElement element = parseStackTraceLine(line);
        if (element == null) return null;

        // We don't want to rely on FqName here, since this name can contain dollar signs
        String fullyQualifiedName = element.getClassName();
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        String classNameWithInners = fullyQualifiedName.substring(lastDot + 1);
        final String packageName = lastDot >= 0 ? fullyQualifiedName.substring(0, lastDot) : "";

        // All classes except 'namespace' and its inner classes are handled correctly in the default ExceptionFilter
        if (!classNameWithInners.equals(JvmAbi.PACKAGE_CLASS) && !classNameWithInners.startsWith(JvmAbi.PACKAGE_CLASS + "$")) {
            return null;
        }

        // Only consider files with the file name from the stack trace and in the given package
        Collection<JetFile> files = Collections2
                .filter(JetFilesProvider.getInstance(project).allInScope(searchScope), new Predicate<JetFile>() {
                    @Override
                    public boolean apply(@Nullable JetFile file) {
                        return file != null
                               && file.getName().equals(element.getFileName())
                               && JetPsiUtil.getFQName(file).getFqName().equals(packageName);
                    }
                });

        if (files.isEmpty()) return null;

        if (files.size() == 1) {
            JetFile file = files.iterator().next();
            VirtualFile virtualFile = file.getVirtualFile();
            return virtualFile == null ? null : new OpenFileHyperlinkInfo(project, virtualFile, element.getLineNumber() - 1);
        }

        // TODO multiple files with the same name within the same package

        return null;
    }

    // Matches strings like "\tat test.namespace$foo$f$1.invoke(a.kt:3)\n"
    private static final Pattern STACK_TRACE_ELEMENT_PATTERN = Pattern.compile("^\\s*at\\s+(.+)\\.(.+)\\((.+):(\\d+)\\)\\s*$");

    @Nullable
    private StackTraceElement parseStackTraceLine(@NotNull String line) {
        Matcher matcher = STACK_TRACE_ELEMENT_PATTERN.matcher(line);
        if (matcher.matches()) {
            String declaringClass = matcher.group(1);
            String methodName = matcher.group(2);
            String fileName = matcher.group(3);
            int lineNumber = Integer.parseInt(matcher.group(4));
            return new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
        }
        return null;
    }

    @NotNull
    private Result patchResult(@NotNull Result result, @NotNull String line) {
        HyperlinkInfo newHyperlinkInfo = createHyperlinkInfo(line);
        return newHyperlinkInfo == null ? result :
               new Result(result.highlightStartOffset, result.highlightEndOffset, newHyperlinkInfo, result.highlightAttributes);
    }

    @Nullable
    @Override
    public Result applyFilter(String line, int entireLength) {
        Result result = exceptionFilter.applyFilter(line, entireLength);
        return result == null ? null : patchResult(result, line);
    }
}
