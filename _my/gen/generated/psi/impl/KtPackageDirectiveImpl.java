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

public class KtPackageDirectiveImpl extends ASTWrapperPsiElement implements KtPackageDirective {

  public KtPackageDirectiveImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitPackageDirective(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public KtDotIdentifier getDotIdentifier() {
    return findChildByClass(KtDotIdentifier.class);
  }

  @Override
  @NotNull
  public List<KtClassDeclaration> getClassDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtClassDeclaration.class);
  }

  @Override
  @NotNull
  public List<KtFunction> getFunctionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtFunction.class);
  }

  @Override
  @NotNull
  public List<KtImportDirective> getImportDirectiveList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtImportDirective.class);
  }

  @Override
  @NotNull
  public List<KtObject> getObjectList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtObject.class);
  }

  @Override
  @NotNull
  public List<KtPackageDirective> getPackageDirectiveList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtPackageDirective.class);
  }

  @Override
  @NotNull
  public List<KtProperty> getPropertyList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtProperty.class);
  }

  @Override
  @Nullable
  public KtReferenceExpression getReferenceExpression() {
    return findChildByClass(KtReferenceExpression.class);
  }

  @Override
  @NotNull
  public List<KtTypedef> getTypedefList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtTypedef.class);
  }

}
