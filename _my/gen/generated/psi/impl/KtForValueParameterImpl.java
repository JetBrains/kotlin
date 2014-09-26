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

public class KtForValueParameterImpl extends ASTWrapperPsiElement implements KtForValueParameter {

  public KtForValueParameterImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitForValueParameter(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<KtAnnotationEntry> getAnnotationEntryList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAnnotationEntry.class);
  }

  @Override
  @NotNull
  public List<KtLongAnnotation> getLongAnnotationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtLongAnnotation.class);
  }

  @Override
  @Nullable
  public KtMultipleVariableDeclarations getMultipleVariableDeclarations() {
    return findChildByClass(KtMultipleVariableDeclarations.class);
  }

  @Override
  @Nullable
  public KtType getType() {
    return findChildByClass(KtType.class);
  }

}
