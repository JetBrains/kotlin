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

public class KtDelegatorSuperCallImpl extends ASTWrapperPsiElement implements KtDelegatorSuperCall {

  public KtDelegatorSuperCallImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitDelegatorSuperCall(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public KtConstructorCallee getConstructorCallee() {
    return findNotNullChildByClass(KtConstructorCallee.class);
  }

  @Override
  @Nullable
  public KtFunctionLiteral getFunctionLiteral() {
    return findChildByClass(KtFunctionLiteral.class);
  }

  @Override
  @Nullable
  public KtLabel getLabel() {
    return findChildByClass(KtLabel.class);
  }

  @Override
  @Nullable
  public KtTypeArguments getTypeArguments() {
    return findChildByClass(KtTypeArguments.class);
  }

  @Override
  @Nullable
  public KtValueArguments getValueArguments() {
    return findChildByClass(KtValueArguments.class);
  }

}
