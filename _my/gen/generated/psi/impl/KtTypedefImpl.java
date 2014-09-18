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

public class KtTypedefImpl extends ASTWrapperPsiElement implements KtTypedef {

  public KtTypedefImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitTypedef(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public KtModifiersPlus getModifiersPlus() {
    return findChildByClass(KtModifiersPlus.class);
  }

  @Override
  @NotNull
  public KtType getType() {
    return findNotNullChildByClass(KtType.class);
  }

  @Override
  @Nullable
  public KtTypeConstraintExt getTypeConstraintExt() {
    return findChildByClass(KtTypeConstraintExt.class);
  }

  @Override
  @Nullable
  public KtTypeParameters getTypeParameters() {
    return findChildByClass(KtTypeParameters.class);
  }

}
