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

public class KtNamedInfixPlusImpl extends ASTWrapperPsiElement implements KtNamedInfixPlus {

  public KtNamedInfixPlusImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitNamedInfixPlus(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<KtCharacterConstant> getCharacterConstantList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtCharacterConstant.class);
  }

  @Override
  @NotNull
  public List<KtFloatConstant> getFloatConstantList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtFloatConstant.class);
  }

  @Override
  @NotNull
  public List<KtIntegerConstant> getIntegerConstantList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtIntegerConstant.class);
  }

  @Override
  @NotNull
  public List<KtNull> getNullList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtNull.class);
  }

  @Override
  @NotNull
  public List<KtAdditiveExpressionPlus> getAdditiveExpressionPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAdditiveExpressionPlus.class);
  }

  @Override
  @NotNull
  public List<KtAnnotatedExpression> getAnnotatedExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtAnnotatedExpression.class);
  }

  @Override
  @NotNull
  public List<KtArrayAccess> getArrayAccessList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtArrayAccess.class);
  }

  @Override
  @NotNull
  public List<KtBinaryConstant> getBinaryConstantList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtBinaryConstant.class);
  }

  @Override
  @NotNull
  public List<KtCallSuffix> getCallSuffixList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtCallSuffix.class);
  }

  @Override
  @NotNull
  public List<KtCallableReference> getCallableReferenceList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtCallableReference.class);
  }

  @Override
  @NotNull
  public List<KtDotQualifiedExpression> getDotQualifiedExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtDotQualifiedExpression.class);
  }

  @Override
  @NotNull
  public List<KtElvisAccessExpression> getElvisAccessExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtElvisAccessExpression.class);
  }

  @Override
  @NotNull
  public List<KtFunctionLiteralExpression> getFunctionLiteralExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtFunctionLiteralExpression.class);
  }

  @Override
  @NotNull
  public List<KtIfExpression> getIfExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtIfExpression.class);
  }

  @Override
  @NotNull
  public KtInOperation getInOperation() {
    return findNotNullChildByClass(KtInOperation.class);
  }

  @Override
  @NotNull
  public List<KtJumpBreak> getJumpBreakList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtJumpBreak.class);
  }

  @Override
  @NotNull
  public List<KtJumpContinue> getJumpContinueList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtJumpContinue.class);
  }

  @Override
  @NotNull
  public List<KtJumpReturn> getJumpReturnList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtJumpReturn.class);
  }

  @Override
  @NotNull
  public List<KtJumpThrow> getJumpThrowList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtJumpThrow.class);
  }

  @Override
  @NotNull
  public List<KtLabel> getLabelList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtLabel.class);
  }

  @Override
  @NotNull
  public List<KtLabelExpression> getLabelExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtLabelExpression.class);
  }

  @Override
  @NotNull
  public List<KtLoop> getLoopList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtLoop.class);
  }

  @Override
  @NotNull
  public List<KtMultiplicativeExpressionPlus> getMultiplicativeExpressionPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtMultiplicativeExpressionPlus.class);
  }

  @Override
  @Nullable
  public KtNamedInfixPlus getNamedInfixPlus() {
    return findChildByClass(KtNamedInfixPlus.class);
  }

  @Override
  @NotNull
  public List<KtObjectLiteral> getObjectLiteralList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtObjectLiteral.class);
  }

  @Override
  @NotNull
  public List<KtParenthesizedExpression> getParenthesizedExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtParenthesizedExpression.class);
  }

  @Override
  @NotNull
  public List<KtPlusPlusAndOthersExpression> getPlusPlusAndOthersExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtPlusPlusAndOthersExpression.class);
  }

  @Override
  @NotNull
  public List<KtPrefixOperationExpression> getPrefixOperationExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtPrefixOperationExpression.class);
  }

  @Override
  @NotNull
  public List<KtReferenceExpression> getReferenceExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtReferenceExpression.class);
  }

  @Override
  @NotNull
  public List<KtSafeAccessExpression> getSafeAccessExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtSafeAccessExpression.class);
  }

  @Override
  @NotNull
  public List<KtStringTemplate> getStringTemplateList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtStringTemplate.class);
  }

  @Override
  @NotNull
  public List<KtSuperExpression> getSuperExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtSuperExpression.class);
  }

  @Override
  @NotNull
  public List<KtThisExpression> getThisExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtThisExpression.class);
  }

  @Override
  @NotNull
  public List<KtTryBlock> getTryBlockList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtTryBlock.class);
  }

  @Override
  @NotNull
  public List<KtTypeRHSPlus> getTypeRHSPlusList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtTypeRHSPlus.class);
  }

  @Override
  @NotNull
  public List<KtWhen> getWhenList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtWhen.class);
  }

}
