package org.jetbrains.jet.plugin.actions;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.checkers.CheckerTestUtil;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.*;

import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author abreslav
 */
public class ShiftAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        assert editor != null && psiFile != null;

//        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) psiFile);
//        List<TextRange> syntaxError = AnalyzingUtils.getSyntaxErrorRanges(psiFile);
//
//        String result = CheckerTestUtil.addDiagnosticMarkersToText(psiFile, bindingContext, syntaxError).toString();
//
//        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//        clipboard.setContents(new StringSelection(result), new ClipboardOwner() {
//            @Override
//            public void lostOwnership(Clipboard clipboard, Transferable contents) {}
//        });
//        final JetFile file = JetPsiFactory.createFile(psiFile.getProject(), "namespace foo\nclass A");
        Project project = psiFile.getProject();

        File rootDir = new File("/Users/abreslav/work/jet-clean/compiler/testData/diagnostics/tests");
        assert rootDir.exists() : "Root dir does not exist";

        System.out.println("Shift started");
        transformDir(project, rootDir);
        System.out.println("Shift done");
    }

    private void transformDir(Project project, File rootDir) {
        assert rootDir.isDirectory();
        println("----------------------------------------------");
        println("Directory: ", rootDir.getAbsolutePath());
        println("----------------------------------------------");
        for (final File file : rootDir.listFiles()) {
            if (file.getName().endsWith(".kt") || file.getName().endsWith(".jet.jet")) {
                file.delete();
            }
        }

        for (File file : rootDir.listFiles()) {
            if (file.isDirectory()) continue;
            if (!file.getName().endsWith(".jet")) continue;
            transformFile(project, file);
        }
        for (File file : rootDir.listFiles()) {
            if (!file.isDirectory()) continue;
            transformDir(project, file);
        }
        println("----------------------------------------------");
        println("----------------------------------------------");
        println("----------------------------------------------");
    }

    private static void transformFile(Project project, File file) {
        println("File name: ", file.getAbsolutePath());
        println("==============================================================");
        System.out.flush();
        try {
            String text = FileUtil.loadTextAndClose(new FileReader(file));
            List<CheckerTestUtil.DiagnosedRange> result = Lists.newArrayList();

            String clearText = CheckerTestUtil.parseDiagnosedRanges(text, result);

            final JetFile jetFile = JetPsiFactory.createFile(project, clearText);
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    convert(jetFile);
                }
            });
            Collection<Diagnostic> diagnostics = Lists.newArrayList();

            for (CheckerTestUtil.DiagnosedRange diagnosedRange : result) {
                for (String message : diagnosedRange.getDiagnostics()) {
                    diagnostics.add(new MyDiagnostic(message, new MyDF(message), new TextRange(diagnosedRange.getStart(), diagnosedRange.getEnd()), jetFile));
                }
            }

            String textWithDiagnosticsBack = CheckerTestUtil.addDiagnosticMarkersToText(jetFile, diagnostics).toString();
            StringBuilder finalText = new StringBuilder();
            for (int i = 0; i < textWithDiagnosticsBack.length(); i++) {
                char c = textWithDiagnosticsBack.charAt(i);
                switch (c) {
                    case 'Я':
                        break;
                    case 'Ц':
                        finalText.append("->");
                        break;
                    case 'Щ':
                        finalText.append("#(");
                        break;
                    case 'Ф':
                        if ("ФАН".equals(textWithDiagnosticsBack.substring(i, i + 3))) {
                            i += 2;
                            while (i < textWithDiagnosticsBack.length() && textWithDiagnosticsBack.charAt(i) == ' ') {
                                i++;
                            }
                        }
                        break;
                    default:
                        finalText.append(c);
                }
            }
            File newFile = new File(file.getAbsoluteFile().getParent(), file.getName() + ".kt");
            FileUtil.writeToFile(newFile, finalText.toString());
            String message = "newFile = " + newFile;
            println(message);
        } catch (Throwable e1) {
            System.err.println(file.getAbsolutePath());
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            System.err.flush();
        }
        println("==============================================================");
    }

    private static void convert(JetFile file) {
        file.getRootNamespace().accept(new JetTreeVisitor<Void>() {

            private void replaceWithLeaf(@NotNull ASTNode toReplace, IElementType type, String text) {
                ASTNode treeNext = toReplace.getTreeNext();
                ASTNode treeParent = toReplace.getTreeParent();
                treeParent.removeChild(toReplace);
                treeParent.addLeaf(type, text, treeNext);
            }

            // namespace -> package
            @Override
            public Void visitNamespace(JetNamespace namespace, Void data) {
                ASTNode namespaceNode = namespace.getHeader().getNode();
                ASTNode token = namespaceNode.findChildByType(NAMESPACE_KEYWORD);
                if (token != null) {
//                    ASTNode packageKeyword = JetPsiFactory.createFile(namespace.getProject(), "package foo").getRootNamespace().getHeader().getNode().findChildByType(NAMESPACE_KEYWORD);
                    replaceWithLeaf(token, NAMESPACE_KEYWORD, "packageЯЯ");
                }
                return super.visitNamespace(namespace, data);
            }

            // function types
            @Override
            public Void visitFunctionType(JetFunctionType type, Void data) {
                ASTNode node = type.getNode();
                ASTNode funToken = node.findChildByType(FUN_KEYWORD);
                replaceWithLeaf(funToken, WHITE_SPACE, "ФАН");

//                node.removeChild(funToken);
                ASTNode colonToken = node.findChildByType(COLON);
                if (colonToken != null) {
//                    JetBinaryExpression arrowExpression = (JetBinaryExpression) JetPsiFactory.createExpression(type.getProject(), "a -> b");
//                    ASTNode childByType = arrowExpression.getOperationReference().getNode().findChildByType(JetTokens.ARROW);
                    replaceWithLeaf(colonToken, WHITE_SPACE, "Ц"); // arrow
                }
                return super.visitFunctionType(type, data);
            }

            // function literals => ->>> ->
            @Override
            public Void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, Void data) {
                ASTNode arrowNode = expression.getFunctionLiteral().getArrowNode();
                if (arrowNode != null) {
                    replaceWithLeaf(arrowNode, ARROW, "->");
                }
                return super.visitFunctionLiteralExpression(expression, data);
            }

            // when entry => ->>> ->
            @Override
            public Void visitWhenEntry(JetWhenEntry jetWhenEntry, Void data) {
                ASTNode arrow = jetWhenEntry.getNode().findChildByType(DOUBLE_ARROW);
                if (arrow == null) {
                    System.err.println(jetWhenEntry.getText());
                }
                else {
                    replaceWithLeaf(arrow, ARROW, "->");
                }
                return super.visitWhenEntry(jetWhenEntry, data);
            }

            // tuple expressions #()
            @Override
            public Void visitTupleExpression(JetTupleExpression expression, Void data) {
                replaceWithLeaf(expression.getNode().getFirstChildNode(), LPAR, "Щ"); // LPAR
                return super.visitTupleExpression(expression, data);
            }

            // typle types #()
            @Override
            public Void visitTupleType(JetTupleType type, Void data) {
                replaceWithLeaf(type.getNode().getFirstChildNode(), LPAR, "Щ"); // LPAR
                return super.visitTupleType(type, data);
            }

            // tuple patterns #()
            @Override
            public Void visitTuplePattern(JetTuplePattern pattern, Void data) {
                replaceWithLeaf(pattern.getNode().getFirstChildNode(), LPAR, "Щ"); // LPAR
                return super.visitTuplePattern(pattern, data);
            }

            // decomposer patterns remove '@'
            @Override
            public Void visitDecomposerPattern(JetDecomposerPattern pattern, Void data) {
                replaceWithLeaf(pattern.getNode().findChildByType(AT), WHITE_SPACE, "Я"); // LPAR
                return super.visitDecomposerPattern(pattern, data);
            }


        }, null);
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(editor != null && psiFile instanceof JetFile && ApplicationManager.getApplication().isInternal());
    }

    private static class MyDiagnostic implements Diagnostic {

        private final String message;
        private final DiagnosticFactory factory;
        private final TextRange range;
        private final PsiFile file;

        private MyDiagnostic(String message, DiagnosticFactory factory, TextRange range, PsiFile file) {
            this.message = message;
            this.factory = factory;
            this.range = range;
            this.file = file;
        }

        public PsiFile getFile() {
            return file;
        }

        @NotNull
        @Override
        public DiagnosticFactory getFactory() {
            return factory;
        }

        @NotNull
        @Override
        public String getMessage() {
            return message;
        }

        public TextRange getRange() {
            return range;
        }

        @NotNull
        @Override
        public Severity getSeverity() {
            return Severity.ERROR;
        }
    }
    
    private static class MyDF implements DiagnosticFactory {

        private String name;

        private MyDF(String name) {
            this.name = name;
        }

        @NotNull
        @Override
        public TextRange getTextRange(@NotNull Diagnostic diagnostic) {
            return ((MyDiagnostic) diagnostic).getRange();
        }

        @NotNull
        @Override
        public PsiFile getPsiFile(@NotNull Diagnostic diagnostic) {
            return ((MyDiagnostic) diagnostic).getFile();
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }
    }

    private static boolean printingOn() {
        return false;
//        return true;
    }

    private static void print(Object message) {
        if (printingOn()) {
            System.out.print(message);
        }
    }

    private static void println() {
        if (printingOn()) {
            System.out.println();
        }
    }

    private static void println(Object message) {
        print(message);
        println();
    }

    private static void println(Object message1, Object message2) {
        print(message1.toString() + message2);
        println();
    }

    private static void println(Object... message) {
        for (Object m : message) {
            print(m);
        }
        println();
    }


}
