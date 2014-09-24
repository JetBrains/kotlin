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

public class KtIfExpressionImpl extends ASTWrapperPsiElement implements KtIfExpression {

  public KtIfExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitIfExpression(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public KtCondition getCondition() {
    return findNotNullChildByClass(KtCondition.class);
  }

  @Override
  @Nullable
  public KtElseExpression getElseExpression() {
    return findChildByClass(KtElseExpression.class);
  }

  @Override
  @Nullable
  public KtThenExpression getThenExpression() {
    return findChildByClass(KtThenExpression.class);
  }

  @Override
  @Nullable
  public KtThenExpressionWithSemi getThenExpressionWithSemi() {
    return findChildByClass(KtThenExpressionWithSemi.class);
  }

}
