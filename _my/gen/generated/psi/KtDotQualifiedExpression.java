// This is a generated file. Not intended for manual editing.
package generated.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface KtDotQualifiedExpression extends PsiElement {

  @NotNull
  List<KtFloatConstant> getFloatConstantList();

  @NotNull
  List<KtIntegerConstant> getIntegerConstantList();

  @NotNull
  List<KtArrayAccess> getArrayAccessList();

  @NotNull
  List<KtBinaryConstant> getBinaryConstantList();

  @NotNull
  List<KtCallSuffix> getCallSuffixList();

  @NotNull
  List<KtCallableReference> getCallableReferenceList();

  @Nullable
  KtDotQualifiedExpression getDotQualifiedExpression();

  @NotNull
  List<KtElvisAccessExpression> getElvisAccessExpressionList();

  @NotNull
  List<KtFunctionLiteralExpression> getFunctionLiteralExpressionList();

  @NotNull
  List<KtIfExpression> getIfExpressionList();

  @NotNull
  List<KtJumpBreak> getJumpBreakList();

  @NotNull
  List<KtJumpContinue> getJumpContinueList();

  @NotNull
  List<KtJumpReturn> getJumpReturnList();

  @NotNull
  List<KtJumpThrow> getJumpThrowList();

  @NotNull
  List<KtLabel> getLabelList();

  @NotNull
  List<KtLoop> getLoopList();

  @NotNull
  List<KtObjectLiteral> getObjectLiteralList();

  @NotNull
  List<KtParenthesizedExpression> getParenthesizedExpressionList();

  @NotNull
  List<KtReferenceExpression> getReferenceExpressionList();

  @NotNull
  List<KtSafeAccessExpression> getSafeAccessExpressionList();

  @NotNull
  List<KtStringTemplate> getStringTemplateList();

  @NotNull
  List<KtThisExpression> getThisExpressionList();

  @NotNull
  List<KtTryBlock> getTryBlockList();

  @Nullable
  KtType getType();

  @NotNull
  List<KtWhen> getWhenList();

}
