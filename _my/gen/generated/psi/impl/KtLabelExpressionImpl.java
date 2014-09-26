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

public class KtLabelExpressionImpl extends ASTWrapperPsiElement implements KtLabelExpression {

  public KtLabelExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof KtVisitor) ((KtVisitor)visitor).visitLabelExpression(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public KtCharacterConstant getCharacterConstant() {
    return findChildByClass(KtCharacterConstant.class);
  }

  @Override
  @Nullable
  public KtFloatConstant getFloatConstant() {
    return findChildByClass(KtFloatConstant.class);
  }

  @Override
  @Nullable
  public KtIntegerConstant getIntegerConstant() {
    return findChildByClass(KtIntegerConstant.class);
  }

  @Override
  @Nullable
  public KtNull getNull() {
    return findChildByClass(KtNull.class);
  }

  @Override
  @Nullable
  public KtAnnotatedExpression getAnnotatedExpression() {
    return findChildByClass(KtAnnotatedExpression.class);
  }

  @Override
  @Nullable
  public KtArrayAccess getArrayAccess() {
    return findChildByClass(KtArrayAccess.class);
  }

  @Override
  @Nullable
  public KtAtomicPackage getAtomicPackage() {
    return findChildByClass(KtAtomicPackage.class);
  }

  @Override
  @Nullable
  public KtBinaryConstant getBinaryConstant() {
    return findChildByClass(KtBinaryConstant.class);
  }

  @Override
  @Nullable
  public KtCallSuffix getCallSuffix() {
    return findChildByClass(KtCallSuffix.class);
  }

  @Override
  @Nullable
  public KtCallableReference getCallableReference() {
    return findChildByClass(KtCallableReference.class);
  }

  @Override
  @Nullable
  public KtDoWhileLoop getDoWhileLoop() {
    return findChildByClass(KtDoWhileLoop.class);
  }

  @Override
  @Nullable
  public KtDotQualifiedExpression getDotQualifiedExpression() {
    return findChildByClass(KtDotQualifiedExpression.class);
  }

  @Override
  @Nullable
  public KtElvisAccessExpression getElvisAccessExpression() {
    return findChildByClass(KtElvisAccessExpression.class);
  }

  @Override
  @Nullable
  public KtFieldIdentifierReference getFieldIdentifierReference() {
    return findChildByClass(KtFieldIdentifierReference.class);
  }

  @Override
  @Nullable
  public KtForLoop getForLoop() {
    return findChildByClass(KtForLoop.class);
  }

  @Override
  @Nullable
  public KtFunctionLiteralExpression getFunctionLiteralExpression() {
    return findChildByClass(KtFunctionLiteralExpression.class);
  }

  @Override
  @Nullable
  public KtIfExpression getIfExpression() {
    return findChildByClass(KtIfExpression.class);
  }

  @Override
  @Nullable
  public KtJumpBreak getJumpBreak() {
    return findChildByClass(KtJumpBreak.class);
  }

  @Override
  @Nullable
  public KtJumpContinue getJumpContinue() {
    return findChildByClass(KtJumpContinue.class);
  }

  @Override
  @Nullable
  public KtJumpReturn getJumpReturn() {
    return findChildByClass(KtJumpReturn.class);
  }

  @Override
  @Nullable
  public KtJumpThrow getJumpThrow() {
    return findChildByClass(KtJumpThrow.class);
  }

  @Override
  @NotNull
  public List<KtLabel> getLabelList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, KtLabel.class);
  }

  @Override
  @Nullable
  public KtLabelExpression getLabelExpression() {
    return findChildByClass(KtLabelExpression.class);
  }

  @Override
  @Nullable
  public KtObjectLiteral getObjectLiteral() {
    return findChildByClass(KtObjectLiteral.class);
  }

  @Override
  @Nullable
  public KtParenthesizedExpression getParenthesizedExpression() {
    return findChildByClass(KtParenthesizedExpression.class);
  }

  @Override
  @Nullable
  public KtPlusPlusAndOthersExpression getPlusPlusAndOthersExpression() {
    return findChildByClass(KtPlusPlusAndOthersExpression.class);
  }

  @Override
  @Nullable
  public KtPrefixOperationExpression getPrefixOperationExpression() {
    return findChildByClass(KtPrefixOperationExpression.class);
  }

  @Override
  @Nullable
  public KtReferenceExpression getReferenceExpression() {
    return findChildByClass(KtReferenceExpression.class);
  }

  @Override
  @Nullable
  public KtSafeAccessExpression getSafeAccessExpression() {
    return findChildByClass(KtSafeAccessExpression.class);
  }

  @Override
  @Nullable
  public KtStringTemplate getStringTemplate() {
    return findChildByClass(KtStringTemplate.class);
  }

  @Override
  @Nullable
  public KtSuperExpression getSuperExpression() {
    return findChildByClass(KtSuperExpression.class);
  }

  @Override
  @Nullable
  public KtThisExpression getThisExpression() {
    return findChildByClass(KtThisExpression.class);
  }

  @Override
  @Nullable
  public KtTryBlock getTryBlock() {
    return findChildByClass(KtTryBlock.class);
  }

  @Override
  @Nullable
  public KtWhen getWhen() {
    return findChildByClass(KtWhen.class);
  }

  @Override
  @Nullable
  public KtWhileLoop getWhileLoop() {
    return findChildByClass(KtWhileLoop.class);
  }

}
