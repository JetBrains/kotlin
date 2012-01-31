package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.psi.PsiFile;
import com.intellij.psi.stubs.DefaultStubBuilder;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetFileStubImpl;

/**
 * @author Nikolay Krasko
 */
public class JetFileStubBuilder  extends DefaultStubBuilder {
    @Override
    protected StubElement createStubForFile(PsiFile file) {

        if (!(file instanceof JetFile)) return super.createStubForFile(file);

        JetFile jetFile = (JetFile) file;

        // TODO (stubs):
        String packageName = "default";

//        String refText = "";
//
//        final LighterASTNode pkg = LightTreeUtil.firstChildOfType(tree, tree.getRoot(), JavaElementType.PACKAGE_STATEMENT);
//        if (pkg != null) {
//            final LighterASTNode ref = LightTreeUtil.firstChildOfType(tree, pkg, JavaElementType.JAVA_CODE_REFERENCE);
//            if (ref != null) {
//                refText = SourceUtil.getTextSkipWhiteSpaceAndComments(tree, ref);
//
//            }
//        }

        return new PsiJetFileStubImpl(jetFile, StringRef.fromString(packageName));
    }
}
