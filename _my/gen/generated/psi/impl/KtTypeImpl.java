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

public class KtTypeImpl extends ASTWrapperPsiElement implements KtType {

  public KtTypeImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitType(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<KtAnnotationWithShort> getAnnotationWithShortList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAnnotationWithShort.class);
  }

  @Override
  @Nullable
  public KtNullableType getNullableType() {
    return findChildByClass(KtNullableType.class);
  }

  @Override
  @Nullable
  public KtParameterModifiersTypeExt getParameterModifiersTypeExt() {
    return findChildByClass(KtParameterModifiersTypeExt.class);
  }

  @Override
  @Nullable
  public KtType getType() {
    return findChildByClass(KtType.class);
  }

}
