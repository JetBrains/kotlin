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

public class KtPrimaryConstructorModifierListImpl extends ASTWrapperPsiElement implements KtPrimaryConstructorModifierList {

  public KtPrimaryConstructorModifierListImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitPrimaryConstructorModifierList(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<KtAnnotation> getAnnotationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAnnotation.class);
  }

  @Override
  @NotNull
  public List<KtMemberModifier> getMemberModifierList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtMemberModifier.class);
  }

}
