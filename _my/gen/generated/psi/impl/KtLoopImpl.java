// This is a generated file. Not intended for manual editing.
package generated.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static generated.KotlinTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import generated.psi.*;

public class KtLoopImpl extends ASTWrapperPsiElement implements KtLoop {

  public KtLoopImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitLoop(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public KtDoWhileLoop getDoWhileLoop() {
    return findChildByClass(KtDoWhileLoop.class);
  }

  @Override
  @Nullable
  public KtForLoop getForLoop() {
    return findChildByClass(KtForLoop.class);
  }

  @Override
  @Nullable
  public KtWhileLoop getWhileLoop() {
    return findChildByClass(KtWhileLoop.class);
  }

}
