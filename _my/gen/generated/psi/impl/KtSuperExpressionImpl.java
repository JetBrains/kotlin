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

public class KtSuperExpressionImpl extends ASTWrapperPsiElement implements KtSuperExpression {

  public KtSuperExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitSuperExpression(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public KtLabel getLabel() {
    return findChildByClass(KtLabel.class);
  }

  @Override
  @NotNull
  public KtSuperReference getSuperReference() {
    return findNotNullChildByClass(KtSuperReference.class);
  }

  @Override
  @Nullable
  public KtType getType() {
    return findChildByClass(KtType.class);
  }

}
