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
  public List<KtAnnotationEntry> getAnnotationEntryList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAnnotationEntry.class);
  }

  @Override
  @Nullable
  public KtClassBody getClassBody() {
    return findChildByClass(KtClassBody.class);
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
  @NotNull
  public List<KtLongAnnotation> getLongAnnotationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtLongAnnotation.class);
  }

  @Override
  @Nullable
  public KtModifierList getModifierList() {
    return findChildByClass(KtModifierList.class);
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
