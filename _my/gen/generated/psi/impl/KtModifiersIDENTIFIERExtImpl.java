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

public class KtModifiersIDENTIFIERExtImpl extends ASTWrapperPsiElement implements KtModifiersIDENTIFIERExt {

  public KtModifiersIDENTIFIERExtImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitModifiersIDENTIFIERExt(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<KtModifiers> getModifiersList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtModifiers.class);
  }

}
