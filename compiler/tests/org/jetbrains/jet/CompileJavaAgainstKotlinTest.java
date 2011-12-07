package org.jetbrains.jet;

import com.intellij.lang.LanguageASTFactory;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.impl.source.tree.JavaASTFactory;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetLanguage;
import org.junit.Assert;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * @author Stepan Koltsov
 */
public class CompileJavaAgainstKotlinTest extends UsefulTestCase {

    private final File ktFile;
    private final File javaFile;
    private JetCoreEnvironment jetCoreEnvironment;
    private File tmpdir;

    public CompileJavaAgainstKotlinTest(File ktFile) {
        this.ktFile = ktFile;
        Assert.assertTrue(ktFile.getName().endsWith(".kt"));
        this.javaFile = new File(ktFile.getPath().replaceFirst("\\.kt", ".java"));
    }

    private void createMockCoreEnvironment() {
        jetCoreEnvironment = new JetCoreEnvironment(myTestRootDisposable);

        final File rtJar = new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/rt.jar");
        jetCoreEnvironment.addToClasspath(rtJar);
        jetCoreEnvironment.addToClasspath(new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/annotations.jar"));
    }

    @Override
    public String getName() {
        return ktFile.getName();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tmpdir = new File("tmp/" + this.getClass().getSimpleName() + "." + this.getName());
        JetTestUtils.recreateDirectory(tmpdir);
    }

    @Override
    public void tearDown() throws Exception {
        Disposer.dispose(myTestRootDisposable);
    }


    @Override
    protected void runTest() throws Throwable {
        createMockCoreEnvironment();

        LanguageASTFactory.INSTANCE.addExplicitExtension(JavaLanguage.INSTANCE, new JavaASTFactory());


        String text = FileUtil.loadFile(ktFile);

        LightVirtualFile virtualFile = new LightVirtualFile("Hello.kt", JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        GenerationState state = new GenerationState(jetCoreEnvironment.getProject(), ClassBuilderFactory.BINARIES);
        AnalyzingUtils.checkForSyntacticErrors(psiFile);
        state.compile(psiFile);

        ClassFileFactory classFileFactory = state.getFactory();

        CompileEnvironment.writeToOutputDirectory(classFileFactory, tmpdir.getPath());

        Disposer.dispose(myTestRootDisposable);

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, Locale.ENGLISH, Charset.forName("utf-8"));
        try {
            Iterable<? extends JavaFileObject> javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(Collections.singleton(javaFile));
            List<String> options = Arrays.asList(
                    "-classpath", tmpdir.getPath(),
                    "-d", tmpdir.getPath()
                );
            JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, null, options, null, javaFileObjectsFromFiles);

            Assert.assertTrue(task.call());
        } finally {
            fileManager.close();
        }
    }

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/compileJavaAgainstKotlin", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new CompileJavaAgainstKotlinTest(file);
            }
        });

    }

}
