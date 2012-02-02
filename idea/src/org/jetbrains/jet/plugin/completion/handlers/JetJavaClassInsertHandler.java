package org.jetbrains.jet.plugin.completion.handlers;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaPsiClassReferenceElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.quickfix.ImportClassHelper;

/**
 * Handler for inserting java class completion.
 * - Should place import directive if necessary.
 *
 * @author Nikolay Krasko
 */
public class JetJavaClassInsertHandler implements InsertHandler<JavaPsiClassReferenceElement> {
    public static final InsertHandler<JavaPsiClassReferenceElement> JAVA_CLASS_INSERT_HANDLER = new JetJavaClassInsertHandler();

    public void handleInsert(final InsertionContext context, final JavaPsiClassReferenceElement item) {
        if (context.getFile() instanceof JetFile) {
            final JetFile jetFile = (JetFile) context.getFile();
            ImportClassHelper.addImportDirective(item.getQualifiedName(), jetFile);
        }

        // check annotation
        // check auto insert parentheses
        // check auto insert type parameters
    }
}
