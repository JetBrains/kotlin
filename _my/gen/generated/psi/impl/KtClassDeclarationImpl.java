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

public class KtClassDeclarationImpl extends ASTWrapperPsiElement implements KtClassDeclaration {

  public KtClassDeclarationImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitClassDeclaration(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<KtAnnotation> getAnnotationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAnnotation.class);
  }

  @Override
  @Nullable
  public KtClassBodyNoQ getClassBodyNoQ() {
    return findChildByClass(KtClassBodyNoQ.class);
  }

  @Override
  @Nullable
  public KtDelegationSpecifierExt getDelegationSpecifierExt() {
    return findChildByClass(KtDelegationSpecifierExt.class);
  }

  @Override
  @Nullable
  public KtEnumClassBody getEnumClassBody() {
    return findChildByClass(KtEnumClassBody.class);
  }

  @Override
  @Nullable
  public KtModifiersPlus getModifiersPlus() {
    return findChildByClass(KtModifiersPlus.class);
  }

  @Override
  @Nullable
  public KtPrimaryConstructorModifierList getPrimaryConstructorModifierList() {
    return findChildByClass(KtPrimaryConstructorModifierList.class);
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

  @Override
  @Nullable
  public KtValueParameters getValueParameters() {
    return findChildByClass(KtValueParameters.class);
  }

}
