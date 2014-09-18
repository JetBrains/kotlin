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

public class KtInitializerExtImpl extends ASTWrapperPsiElement implements KtInitializerExt {

  public KtInitializerExtImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitInitializerExt(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<KtAnnotation> getAnnotationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAnnotation.class);
  }

  @Override
  @NotNull
  public List<KtDelegatorSuperCall> getDelegatorSuperCallList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtDelegatorSuperCall.class);
  }

  @Override
  @NotNull
  public List<KtValueArguments> getValueArgumentsList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtValueArguments.class);
  }

}
