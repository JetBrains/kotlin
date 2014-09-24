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

public class KtFunctionLiteralImpl extends ASTWrapperPsiElement implements KtFunctionLiteral {

  public KtFunctionLiteralImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitFunctionLiteral(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public KtFunctionLiteralValueParameterList1 getFunctionLiteralValueParameterList1() {
    return findChildByClass(KtFunctionLiteralValueParameterList1.class);
  }

  @Override
  @Nullable
  public KtFunctionLiteralValueParameterList2 getFunctionLiteralValueParameterList2() {
    return findChildByClass(KtFunctionLiteralValueParameterList2.class);
  }

  @Override
  @NotNull
  public KtStatementsBlock getStatementsBlock() {
    return findNotNullChildByClass(KtStatementsBlock.class);
  }

  @Override
  @NotNull
  public List<KtType> getTypeList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtType.class);
  }

}
