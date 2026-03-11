// SKIP_JDK6
// TARGET_BACKEND: JVM
// FULL_JDK

// MODULE: lib
// FILE: PsiElement.java

public interface PsiElement {

}

// FILE: PsiElementProcessor.java

import org.jetbrains.annotations.NotNull;

public interface PsiElementProcessor<T extends PsiElement> {
    boolean execute (@NotNull T element);
}

// FILE: PsiTreeUtil.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiTreeUtil {
    public static boolean processElements(@Nullable PsiElement element, @NotNull PsiElementProcessor processor) {
        return element != null;
    }
}

// MODULE: main(lib)
// FILE: KotlinDocumentationProvider.kt

import java.util.function.Consumer

interface PsiFile : PsiElement {
    val name: String
}

class KtFile(override val name: String) : PsiFile {
    val docComment: PsiDocCommentBase get() = PsiDocCommentBase()
}

class PsiDocCommentBase : PsiElement

fun collectDocComments(file: PsiFile, sink: Consumer<PsiDocCommentBase>): String {
    if (file !is KtFile) return "FAIL"

    PsiTreeUtil.processElements(file) {
        val comment = (it as? KtFile)?.docComment
        if (comment != null) sink.accept(comment)
        true
    }

    return file.name
}

fun box(): String {
    return collectDocComments(KtFile("OK")) {

    }
}
