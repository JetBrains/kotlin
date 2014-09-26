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

public class KtFunctionTypeReceiverImpl extends ASTWrapperPsiElement implements KtFunctionTypeReceiver {

  public KtFunctionTypeReceiverImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitFunctionTypeReceiver(this);
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
  public KtNullableType getNullableType() {
    return findChildByClass(KtNullableType.class);
  }

  @Override
  @Nullable
  public KtSelfType getSelfType() {
    return findChildByClass(KtSelfType.class);
  }

  @Override
  @Nullable
  public KtSimpleUserType getSimpleUserType() {
    return findChildByClass(KtSimpleUserType.class);
  }

  @Override
  @Nullable
  public KtSimpleUserTypeAdd getSimpleUserTypeAdd() {
    return findChildByClass(KtSimpleUserTypeAdd.class);
  }

}
