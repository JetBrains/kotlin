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

public class KtPackageHeaderImpl extends ASTWrapperPsiElement implements KtPackageHeader {

  public KtPackageHeaderImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitPackageHeader(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public KtDotIdentifier getDotIdentifier() {
    return findChildByClass(KtDotIdentifier.class);
  }

  @Override
  @Nullable
  public KtModifierList getModifierList() {
    return findChildByClass(KtModifierList.class);
  }

  @Override
  @Nullable
  public KtReferenceExpression getReferenceExpression() {
    return findChildByClass(KtReferenceExpression.class);
  }

}
