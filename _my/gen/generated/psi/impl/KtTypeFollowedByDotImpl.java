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

public class KtTypeFollowedByDotImpl extends ASTWrapperPsiElement implements KtTypeFollowedByDot {

  public KtTypeFollowedByDotImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitTypeFollowedByDot(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<KtAnnotation> getAnnotationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAnnotation.class);
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
  public KtSelfType getSelfType() {
    return findChildByClass(KtSelfType.class);
  }

  @Override
  @Nullable
  public KtType getType() {
    return findChildByClass(KtType.class);
  }

  @Override
  @Nullable
  public KtUserType getUserType() {
    return findChildByClass(KtUserType.class);
  }

  @Override
  @Nullable
  public KtUserTypeFollowedByDot getUserTypeFollowedByDot() {
    return findChildByClass(KtUserTypeFollowedByDot.class);
  }

}
