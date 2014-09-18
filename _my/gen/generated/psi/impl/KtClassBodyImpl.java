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

public class KtClassBodyImpl extends ASTWrapperPsiElement implements KtClassBody {

  public KtClassBodyImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitClassBody(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<KtAnonymousInitializer> getAnonymousInitializerList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAnonymousInitializer.class);
  }

  @Override
  @NotNull
  public List<KtClassDeclaration> getClassDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtClassDeclaration.class);
  }

  @Override
  @NotNull
  public List<KtClassObject> getClassObjectList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtClassObject.class);
  }

  @Override
  @NotNull
  public List<KtFunction> getFunctionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtFunction.class);
  }

  @Override
  @NotNull
  public List<KtObject> getObjectList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtObject.class);
  }

  @Override
  @NotNull
  public List<KtProperty> getPropertyList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtProperty.class);
  }

  @Override
  @NotNull
  public List<KtTypedef> getTypedefList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtTypedef.class);
  }

}
